# 项目上下文

## 项目概览
当前代码仓库的真实可运行产物由以下部分组成：
- `biz-service/`：一个基于 Spring Boot 3.4.0、Java 21、Maven 的业务服务，现已接入 PostgreSQL 与 Redis，并提供真实读写接口；
- `ui-service/`：一个基于相同技术栈的前端交互服务，同样接入 PostgreSQL 与 Redis，并提供前端交互型真实读写接口；
- `nginx/nginx.conf`：统一 HTTP 入口的反向代理配置；
- `docker-compose.yml`：一个根目录的本地开发编排基线，可同时启动 `postgres`、`redis`、`biz-service`、`ui-service`、`nginx`；
- `.helloagents/`：项目知识库与方案包归档目录。

## 当前真实目录边界
| 层次 | 当前组件 | 关键路径 | 状态 |
| --- | --- | --- | --- |
| 应用层 | `biz-service` | `biz-service/` | 已落地 |
| 应用层 | `ui-service` | `ui-service/` | 已落地 |
| 网关层 | `nginx` | `nginx/nginx.conf` | 已落地 |
| 部署层 | Docker Compose 基线 | `docker-compose.yml` | 已落地 |
| 知识库 | HelloAGENTS KB | `.helloagents/` | 已落地 |
| 历史规划 | Swarm、监控与自动扩缩容 | `.helloagents/archive/` 与旧模块文档 | 仅文档参考，当前代码缺失 |

## 已落地事实
1. `biz-service/pom.xml` 与 `ui-service/pom.xml` 都固定使用 Spring Boot 3.4.0、Java 21，并包含 `spring-boot-starter-web`、`spring-boot-starter-actuator`、`spring-boot-starter-jdbc`、`spring-boot-starter-data-redis` 与 PostgreSQL Driver。
2. `biz-service` 当前提供：`GET /api/health`、`GET /api/dependencies`、`GET /api/test`、`POST /api/pg/notes`、`GET /api/pg/notes/{id}`、`GET /api/pg/notes`、`POST /api/redis/kv`、`GET /api/redis/kv/{key}`。
3. `ui-service` 当前提供：`GET /`、`GET /ui-api/health`、`GET /ui-api/dependencies`、`GET /ui-api/test`、`POST /ui-api/preferences`、`GET /ui-api/preferences/{userId}`、`POST /ui-api/sessions`、`GET /ui-api/sessions/{sessionId}`。
4. `biz-service` 通过 PostgreSQL `notes` 表与 Redis KV 接口承载业务数据处理；`ui-service` 通过 PostgreSQL `ui_preferences` 表与 Redis `ui:session:*` key 前缀承载前端交互数据。
5. 两个服务都实现了 `StartupState`、自定义健康接口、依赖验证接口与 WebMvc 测试，并通过 Docker 多阶段构建生成镜像。
6. `docker-compose.yml` 当前定义了 `postgres`、`redis`、`biz-service`、`ui-service`、`nginx` 五个服务；其中 `nginx` 作为统一入口，对外暴露 `80`，并通过 `/api/* -> biz-service`、`/ui-api/* 与 / -> ui-service` 完成分流。
7. `nginx/nginx.conf` 还额外提供 `GET /nginx/health` 作为自身健康检查路径，并透传 `Host`、`X-Forwarded-For`、`X-Forwarded-Proto`、`X-Request-Id` 等代理头。

## 验证事实
1. 宿主机未安装 Java / Maven，本轮继续通过 Docker 多阶段构建完成 Maven 编译、测试与打包。
2. `docker-compose config --services` 已确认当前编排包含 5 个服务：`postgres`、`redis`、`ui-service`、`biz-service`、`nginx`。
3. `docker-compose up -d --build` 已成功启动上述 5 个服务，`docker-compose ps` 显示 5 个容器均为 `healthy`。
4. 已通过 nginx 入口验证：
   - `GET /` 可返回 ui-service 摘要；
   - `GET /ui-api/health` 可返回 ui-service 与 PostgreSQL/Redis 的依赖状态；
   - `GET /api/health` 可返回 biz-service 与 PostgreSQL/Redis 的依赖状态。
5. 已通过 nginx 入口完成 UI 服务真实写读验证：
   - `POST /ui-api/preferences` 与 `GET /ui-api/preferences/{userId}` 可对 PostgreSQL `ui_preferences` 执行真实写读；
   - `POST /ui-api/sessions` 与 `GET /ui-api/sessions/{sessionId}` 可对 Redis `ui:session:*` 执行真实写读。
6. 已通过 nginx 入口回归验证 Biz 服务真实写读：
   - `POST /api/pg/notes` 与 `GET /api/pg/notes/{id}` 可继续对 PostgreSQL `notes` 表执行真实写读；
   - `POST /api/redis/kv` 与 `GET /api/redis/kv/{key}` 可继续对 Redis 执行真实写读。

## 历史规划说明
- 旧知识库中关于 `deploy/swarm/`、监控与自动扩缩容的表述来自先前方案包与规划沉淀。
- 当前工作区并不存在上述目录，因此这些内容仅可视为历史规划/参考背景，不能视为当前代码事实。
- 后续若重新落地对应目录，应以实际代码再次同步知识库。

## 关联决策
- springboot-biz-service-bootstrap#D001：采用单服务 Spring Boot + 自定义健康接口 + Docker 多阶段构建初始化业务服务基线。
- pg-redis-compose-bootstrap#D001：采用 JDBC + Redis 轻量探测，而不是引入完整持久化业务层。
- biz-service-real-readwrite#D001：采用 JdbcTemplate 实现 PostgreSQL notes 最小真实读写，而不是直接升级为 JPA。
- ui-biz-nginx-compose-gateway#D001：采用职责分层双直连，而不是让 ui-service 完全不连数据库。
- ui-biz-nginx-compose-gateway#D002：保持 biz-service `/api/*` 路由不变，并为 ui-service 新增 `/ui-api/*`。
