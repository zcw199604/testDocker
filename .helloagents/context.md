# 项目上下文

## 项目概览
当前代码仓库的真实可运行产物由以下部分组成：
- `biz-service/`：基于 Spring Boot 3.4.0、Java 21、Maven 的业务服务，已接入 PostgreSQL 与 Redis，并提供 autoscale 指标接口；
- `ui-service/`：基于相同技术栈的前端交互服务；
- `nginx/nginx.legacy.conf`：旧版单机 5 服务入口使用的静态网关配置；
- `nginx/nginx.conf` + `nginx/generated/`：autoscale 入口使用的动态 upstream 网关配置；
- `autoscale-agent/`：基于 Python 3.12 的本地自动扩缩容协调器；
- `docker-compose.yml`：旧版单机 5 服务入口；
- `docker-compose.autoscale.yml`：本地自动扩缩容 6 服务入口；
- `.helloagents/`：知识库、方案包与归档目录。

## 当前真实目录边界
| 层次 | 当前组件 | 关键路径 | 状态 |
| --- | --- | --- | --- |
| 应用层 | `biz-service` | `biz-service/` | 已落地 |
| 应用层 | `ui-service` | `ui-service/` | 已落地 |
| 网关层 | `nginx` | `nginx/nginx.legacy.conf`、`nginx/nginx.conf`、`nginx/generated/` | 已落地 |
| 控制层 | `autoscale-agent` | `autoscale-agent/` | 已落地 |
| 部署层 | Docker Compose | `docker-compose.yml`、`docker-compose.autoscale.yml` | 已落地 |
| 知识库 | HelloAGENTS KB | `.helloagents/` | 已落地 |
| 历史规划 | Swarm / Prometheus / 旧监控方案 | `.helloagents/archive/`、旧模块文档 | 仅历史参考 |

## 已落地事实
1. 当前存在两套 Compose 入口：`docker-compose.yml` 与 `docker-compose.autoscale.yml`。二者共用 `name: testdocker`，并复用相同端口、服务名与部分固定容器名，属于替代关系，不建议同时启动。
2. `docker-compose.yml` 当前定义 5 个服务：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`。
3. 旧版 `docker-compose.yml` 中，`biz-service` 恢复了固定 `container_name: testdocker-biz-service-compose` 与宿主机端口 `8080:8080`。
4. 旧版 `docker-compose.yml` 中，`nginx` 挂载 `./nginx/nginx.legacy.conf:/etc/nginx/nginx.conf:ro`；`nginx.legacy.conf` 静态定义 `biz_service_upstream`，`/api/*` 代理到 `http://biz_service_upstream`。
5. `docker-compose.autoscale.yml` 当前定义 6 个服务：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`、`autoscale-agent`。
6. autoscale 版中，`biz-service` 仅 `expose: 8080`，并带有 autoscale labels 与 `AUTOSCALE_METRICS_WINDOW_DURATION=15s`。
7. autoscale 版中，`nginx` 挂载 `./nginx/nginx.conf` 与 `./nginx/generated`；`nginx.conf` 通过 `include /etc/nginx/generated/biz-service.upstream.inc;` 引入生成式 `biz_service_upstream`。
8. `autoscale-agent` 当前采用“Compose 配置源 + Docker SDK 直控副本”的实现方式：容器内继续读取 `/workspace/docker-compose.yml`，但宿主机映射源文件已是 `./docker-compose.autoscale.yml`。
9. `autoscale-agent` 不依赖 `docker-compose --scale` 做实时副本控制；Compose 只提供模板与默认环境配置，副本创建 / 摘流 / 删除由 Docker SDK 执行。
10. `biz-service` 继续提供 `GET /api/health`、`GET /api/dependencies`、`GET /api/test`、PostgreSQL / Redis 读写接口，以及 `GET /api/autoscale/metrics`。
11. `biz-service` 已启用 graceful shutdown；`StartupState` 当前可表达 `STARTING`、`READY`、`STOPPING` 生命周期状态。

## 验证事实
1. `docker-compose -f docker-compose.yml config --services` 当前输出 5 个服务：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`。
2. `docker-compose -f docker-compose.autoscale.yml config --services` 当前输出 6 个服务：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`、`autoscale-agent`。

## 历史规划说明
- 旧知识库中关于 `deploy/swarm/`、Prometheus、旧 autoscaler 的表述来自先前方案包与规划沉淀。
- 当前工作区真实落地的是基于 `docker-compose`、`nginx` 与 `autoscale-agent` 的本地编排与自动扩缩容闭环，不包含 Swarm / Kubernetes / Prometheus 运行时。
- 后续若继续演进更重编排平台，应以当前两套 Compose 入口的真实代码为基线重新同步知识库，而不是直接沿用旧规划文字。

## 关联决策
- springboot-biz-service-bootstrap#D001：采用单服务 Spring Boot + 自定义健康接口 + Docker 多阶段构建初始化业务服务基线。
- pg-redis-compose-bootstrap#D001：采用 JDBC + Redis 轻量探测，而不是引入完整持久化业务层。
- biz-service-real-readwrite#D001：采用 JdbcTemplate 实现 PostgreSQL notes 最小真实读写，而不是直接升级为 JPA。
- ui-biz-nginx-compose-gateway#D001：采用职责分层双直连，而不是让 ui-service 完全不连数据库。
- ui-biz-nginx-compose-gateway#D002：保持 biz-service `/api/*` 路由不变，并为 ui-service 新增 `/ui-api/*`。
- compose-biz-service-autoscale#D001：选择“Compose 配置源协调型自动扩缩容”。
- compose-biz-service-autoscale#D002：`/api/*` 通过 nginx 生成式 upstream 管理健康副本池。
- compose-biz-service-autoscale#D003：扩缩容执行采用“Compose 作为配置源 + Docker SDK 直控 managed 副本”，而不是依赖 `docker-compose --scale` 作为实时控制面。
