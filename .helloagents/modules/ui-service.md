# ui-service

## 范围
- `ui-service/pom.xml`
- `ui-service/Dockerfile`
- `ui-service/.dockerignore`
- `ui-service/src/main/java/`
- `ui-service/src/main/resources/application.yaml`
- `ui-service/src/main/resources/schema.sql`
- `ui-service/src/test/java/`

## 当前事实
- `ui-service` 是一个基于 Spring Boot 3.4.0、Java 21、Maven 的前端交互服务，当前已接入 PostgreSQL 与 Redis，并具备真实读写能力。
- 服务通过 `spring-boot-starter-web` 提供 HTTP API，通过 `spring-boot-starter-actuator` 暴露 `health` 与 `info` 端点。
- 构建依赖包含 `spring-boot-starter-jdbc`、`spring-boot-starter-data-redis` 与 PostgreSQL JDBC Driver。
- 默认入口为 `GET /`，返回 ui-service 的服务角色与主要路由摘要。
- 自定义健康接口为 `GET /ui-api/health`，返回服务状态、是否完成启动、启动时间、ready 时间、运行时长，以及 PostgreSQL / Redis 的依赖状态。
- 依赖验证接口为 `GET /ui-api/dependencies`，专门返回 PostgreSQL 与 Redis 是否可用、诊断信息与时间戳。
- 测试接口为 `GET /ui-api/test`，支持可选参数 `name`，返回示例问候消息与时间戳。
- PostgreSQL 真实读写接口包括：
  - `POST /ui-api/preferences`
  - `GET /ui-api/preferences/{userId}`
- Redis 真实读写接口包括：
  - `POST /ui-api/sessions`
  - `GET /ui-api/sessions/{sessionId}`
- PostgreSQL 读写通过 `JdbcTemplate` 落地，使用 `schema.sql` 初始化 `ui_preferences` 表；Redis 读写通过 `StringRedisTemplate` 落地，使用 `ui:session:*` 作为 key 前缀，并支持可选 TTL。
- `StartupState` 通过 `ApplicationReadyEvent` 记录服务启动完成状态；`StartupStateHealthIndicator` 会把该状态同步到 Actuator 健康信息中。
- `DependencyStatusService` 使用 `DataSource` 校验 PostgreSQL，并使用 `RedisConnectionFactory` 对 Redis 执行 `PING`。
- `application.yaml` 将应用名固定为 `ui-service`，服务监听 `8080`，并复用与 biz-service 一致的 PostgreSQL / Redis 连接参数风格。
- Docker 镜像通过多阶段构建生成：构建阶段使用 Maven + Temurin 21，运行阶段使用 `eclipse-temurin:21-jre-alpine`。
- `src/test/java/` 下已有 WebMvc 测试，覆盖 `/`、`/ui-api/health`、`/ui-api/dependencies`、`/ui-api/test`、PostgreSQL 偏好接口与 Redis 会话接口的关键返回结构。

## 依赖关系
- 构建依赖：Maven、Spring Boot 3.4.0、Java 21。
- 运行依赖：PostgreSQL、Redis。
- 配置依赖：`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、`SPRING_DATA_REDIS_HOST`、`SPRING_DATA_REDIS_PORT`、`SPRING_SQL_INIT_MODE`。
- 网关依赖：可由 `nginx` 的 `/ui-api/*` 与 `/` 路由代理访问。
- 观测依赖：Actuator 暴露基础健康与信息端点。

## 运行关注点
- 宿主机无 Java / Maven 时，可直接通过 Docker 构建镜像完成编译与测试。
- 默认本地开发配置会把 PostgreSQL 指向 `jdbc:postgresql://localhost:5432/bizdb`，Redis 指向 `localhost:6379`；在 Compose 场景下通过环境变量改为容器服务名。
- `schema.sql` 仅在 `SPRING_SQL_INIT_MODE=always` 时初始化；当前 compose 环境已开启该参数，用于自动创建 `ui_preferences` 表。
- 健康探针建议优先使用 `/ui-api/health`；标准平台探针可补充使用 `/actuator/health`。
- 当前设计允许 `ui-service` 与 `biz-service` 同时接入 PostgreSQL/Redis，但需通过表名与 Redis key 前缀保持职责隔离：`ui-service` 负责前端交互数据，`biz-service` 负责业务处理数据。
