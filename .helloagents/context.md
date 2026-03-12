# 项目上下文

## 项目概览
当前代码仓库的真实可运行产物由以下部分组成：
- `biz-service/`：基于 Spring Boot 3.4.0、Java 21、Maven 的业务服务，已接入 PostgreSQL 与 Redis，并新增 autoscale 指标接口；
- `ui-service/`：基于相同技术栈的前端交互服务；
- `nginx/nginx.conf` + `nginx/generated/`：统一 HTTP 入口与动态 upstream 生成目录；
- `autoscale-agent/`：基于 Python 3.12 的本地自动扩缩容协调器；
- `docker-compose.yml`：当前唯一真实编排入口；
- `.helloagents/`：知识库、方案包与归档目录。

## 当前真实目录边界
| 层次 | 当前组件 | 关键路径 | 状态 |
| --- | --- | --- | --- |
| 应用层 | `biz-service` | `biz-service/` | 已落地 |
| 应用层 | `ui-service` | `ui-service/` | 已落地 |
| 网关层 | `nginx` | `nginx/nginx.conf`、`nginx/generated/` | 已落地 |
| 控制层 | `autoscale-agent` | `autoscale-agent/` | 已落地 |
| 部署层 | Docker Compose | `docker-compose.yml` | 已落地 |
| 知识库 | HelloAGENTS KB | `.helloagents/` | 已落地 |
| 历史规划 | Swarm / Prometheus / 旧监控方案 | `.helloagents/archive/`、旧模块文档 | 仅历史参考 |

## 已落地事实
1. `docker-compose.yml` 当前定义 6 个服务：`postgres`、`redis`、`ui-service`、`biz-service`、`nginx`、`autoscale-agent`。
2. `biz-service` 已移除固定 `container_name` 与宿主机 `8080:8080` 暴露，只通过 Compose 网络 `expose: 8080` 提供服务。
3. `biz-service` 新增 `GET /api/autoscale/metrics`，返回 `serviceName`、`instanceId`、`status`、`startupCompleted`、`inflightRequests`、`windowRequestCount`、`windowErrorCount`、`requestRatePerSecond`、`averageResponseTimeMs`、`windowStartedAt`、`timestamp`。
4. `biz-service` 通过 `AutoscaleMetricsFilter` 统计 `/api/*` 业务请求，并排除 `/api/health`、`/api/dependencies`、`/api/autoscale/metrics`。
5. `biz-service` 已启用 graceful shutdown，`StartupState` 现在可以表达 `STARTING` / `READY` / `STOPPING` 生命周期状态。
6. `nginx/nginx.conf` 当前在 `http {}` 中 `include /etc/nginx/generated/biz-service.upstream.inc;`，`/api/*` 通过 `biz_service_upstream` 代理到健康副本池。
7. `nginx/generated/biz-service.upstream.inc` 由 `autoscale-agent` 原子写入，并在变更前后执行 `nginx -t` + `nginx -s reload`。
8. `autoscale-agent` 当前采用“Compose 配置源 + Docker SDK 直控副本”的实现方式：读取 `docker-compose.yml` 中 `biz-service` 模板，使用 Docker Socket 直接创建 / 摘流 / 删除 managed 副本，而不是把 Compose 当作实时副本控制器。
9. `autoscale-agent` 同时采集容器 CPU / 内存与业务指标 `requestRatePerSecond`、`inflightRequests`，当前阈值通过环境变量配置：
   - `AUTOSCALE_POLL_INTERVAL_SECONDS=5`
   - `AUTOSCALE_SCALE_UP_WINDOW=2`
   - `AUTOSCALE_SCALE_DOWN_WINDOW=2`
   - `AUTOSCALE_COOLDOWN_SECONDS=30`
   - `AUTOSCALE_DRAIN_SECONDS=10`
   - `AUTOSCALE_CPU_UP_THRESHOLD=70`
   - `AUTOSCALE_CPU_DOWN_THRESHOLD=25`
   - `AUTOSCALE_MEMORY_UP_THRESHOLD=80`
   - `AUTOSCALE_MEMORY_DOWN_THRESHOLD=35`
   - `AUTOSCALE_APP_METRIC_RULES=[{"name":"requestRatePerSecond","up":0.2,"down":0.02,"aggregation":"max"},{"name":"inflightRequests","up":2,"down":0,"aggregation":"max"}]`
10. `biz-service` 当前在 Compose 中额外注入 `AUTOSCALE_METRICS_WINDOW_DURATION=15s`，用于本地弹性演练。
11. `autoscale-agent` 代码已支持 `dry_run` 与 `failure_freeze` 配置解析，当前 Compose 默认未开启 `dry_run`，保持自动执行模式。

## 验证事实
1. `docker-compose config --services` 已确认当前编排包含 6 个服务：`postgres`、`redis`、`ui-service`、`biz-service`、`nginx`、`autoscale-agent`。
2. `PYTHONPATH=/mnt/testDocker/autoscale-agent python3 -m unittest discover -s /mnt/testDocker/autoscale-agent/tests -v` 当前通过 8 个 Python 单元测试。
3. `docker run --rm -v "/mnt/testDocker/biz-service:/workspace" -v "/tmp/m2-biz-service:/root/.m2" -w /workspace maven:3.9-eclipse-temurin-21 mvn test` 已通过。
4. `docker-compose up -d --build` 后，`docker-compose ps` 显示 6 个服务均可正常运行，其中 `postgres`、`redis`、`biz-service`、`ui-service`、`nginx` 为 healthy，`autoscale-agent` 为持续运行状态。
5. 已通过入口验证：
   - `GET http://127.0.0.1/nginx/health` 返回 `ok`；
   - `GET http://127.0.0.1/api/health` 返回业务服务健康与 PostgreSQL / Redis 依赖状态；
   - `GET http://127.0.0.1/api/autoscale/metrics` 返回 autoscale 指标快照。
6. 已完成两轮自动扩缩容联调：
   - 2026-03-11 14:44:06 UTC 与 14:45:52 UTC，`autoscale-agent` 因 `requestRatePerSecond` 超阈值自动扩容；
   - 2026-03-11 14:44:15 UTC 与 14:46:07 UTC，扩容副本健康通过后被写入 nginx upstream；
   - 经 nginx 连续访问 `/api/autoscale/metrics`，观测到 2 个不同 `instanceId`，证明新副本已实际承接流量；
   - 2026-03-11 14:44:39 UTC 与 14:46:31 UTC，`autoscale-agent` 先摘流、reload nginx、等待 drain，再自动缩容回落至单副本。

## 历史规划说明
- 旧知识库中关于 `deploy/swarm/`、Prometheus、旧 autoscaler 的表述来自先前方案包与规划沉淀。
- 当前工作区真实落地的是基于 `docker-compose`、`nginx` 与 `autoscale-agent` 的本地自动扩缩容闭环，不包含 Swarm / Kubernetes / Prometheus 运行时。
- 后续若切换到更重编排平台，应以当前落地代码为基线重新同步知识库，而不是直接沿用旧规划文字。

## 关联决策
- springboot-biz-service-bootstrap#D001：采用单服务 Spring Boot + 自定义健康接口 + Docker 多阶段构建初始化业务服务基线。
- pg-redis-compose-bootstrap#D001：采用 JDBC + Redis 轻量探测，而不是引入完整持久化业务层。
- biz-service-real-readwrite#D001：采用 JdbcTemplate 实现 PostgreSQL notes 最小真实读写，而不是直接升级为 JPA。
- ui-biz-nginx-compose-gateway#D001：采用职责分层双直连，而不是让 ui-service 完全不连数据库。
- ui-biz-nginx-compose-gateway#D002：保持 biz-service `/api/*` 路由不变，并为 ui-service 新增 `/ui-api/*`。
- compose-biz-service-autoscale#D001：选择“Compose 配置源协调型自动扩缩容”。
- compose-biz-service-autoscale#D002：`/api/*` 通过 nginx 生成式 upstream 管理健康副本池。
- compose-biz-service-autoscale#D003：扩缩容执行采用“Compose 作为配置源 + Docker SDK 直控 managed 副本”，而不是依赖 `docker-compose --scale` 作为实时控制面。
