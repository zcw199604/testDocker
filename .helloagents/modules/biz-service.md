# biz-service

## 范围
- `biz-service/pom.xml`
- `biz-service/Dockerfile`
- `biz-service/.dockerignore`
- `biz-service/src/main/java/`
- `biz-service/src/main/resources/application.yaml`
- `biz-service/src/main/resources/schema.sql`
- `biz-service/src/test/java/`

## 当前事实
- `biz-service` 是一个基于 Spring Boot 3.4.0、Java 21、Maven 的业务数据处理服务，当前已接入 PostgreSQL 与 Redis，并具备真实读写能力。
- 服务通过 `spring-boot-starter-web` 提供 HTTP API，通过 `spring-boot-starter-actuator` 暴露 `health` 与 `info` 端点。
- 构建依赖包含 `spring-boot-starter-jdbc`、`spring-boot-starter-data-redis` 与 PostgreSQL JDBC Driver。
- 自定义健康接口为 `GET /api/health`，返回服务状态、是否完成启动、启动时间、ready 时间、运行时长，以及 PostgreSQL / Redis 的依赖状态。
- 依赖验证接口为 `GET /api/dependencies`，专门返回 PostgreSQL 与 Redis 是否可用、诊断信息与时间戳。
- 测试接口为 `GET /api/test`，支持可选参数 `name`，返回示例问候消息与时间戳。
- PostgreSQL 真实读写接口包括：
  - `POST /api/pg/notes`
  - `GET /api/pg/notes/{id}`
  - `GET /api/pg/notes`
- Redis 真实读写接口包括：
  - `POST /api/redis/kv`
  - `GET /api/redis/kv/{key}`
- PostgreSQL 读写通过 `JdbcTemplate` 落地，使用 `schema.sql` 初始化 `notes` 表；Redis 读写通过 `StringRedisTemplate` 落地，支持可选 TTL。
- `StartupState` 通过 `ApplicationReadyEvent` 记录服务启动完成状态；`StartupStateHealthIndicator` 会把该状态同步到 Actuator 健康信息中。
- `DependencyStatusService` 使用 `DataSource` 校验 PostgreSQL，并使用 `RedisConnectionFactory` 对 Redis 执行 `PING`，在依赖不可用时返回结构化错误信息而不是抛出未处理异常。
- `application.yaml` 将应用名固定为 `biz-service`，服务监听 `8080`，并配置了 PostgreSQL / Redis 连接参数、Hikari 容错启动参数，以及 `SPRING_SQL_INIT_MODE` 驱动的 SQL 初始化开关。
- Docker 镜像通过多阶段构建生成：构建阶段使用 Maven + Temurin 21，运行阶段使用 `eclipse-temurin:21-jre-alpine`。
- 当前 compose 与 nginx 联调下，`biz-service` 既可通过宿主机 `8080` 直接访问，也可通过 nginx 的 `/api/*` 代理路径访问。
- `src/test/java/` 下已有 WebMvc 测试，覆盖 `/api/health`、`/api/dependencies`、`/api/test`、PostgreSQL notes 接口与 Redis KV 接口的关键返回结构。

## 依赖关系
- 构建依赖：Maven、Spring Boot 3.4.0、Java 21。
- 运行依赖：PostgreSQL、Redis。
- 配置依赖：`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、`SPRING_DATA_REDIS_HOST`、`SPRING_DATA_REDIS_PORT`、`SPRING_SQL_INIT_MODE`。
- 网关依赖：可由 `nginx` 的 `/api/*` 路由代理访问。
- 观测依赖：Actuator 暴露基础健康与信息端点。

## 运行关注点
- 宿主机无 Java / Maven 时，可直接通过 Docker 构建镜像完成编译与测试。
- 默认本地开发配置会把 PostgreSQL 指向 `jdbc:postgresql://localhost:5432/bizdb`，Redis 指向 `localhost:6379`；在 Compose 场景下通过环境变量改为容器服务名。
- `schema.sql` 仅在 `SPRING_SQL_INIT_MODE=always` 时初始化；当前 compose 环境已开启该参数，用于自动创建 `notes` 表。
- 健康探针建议优先使用 `/api/health`；标准平台探针可补充使用 `/actuator/health`。
- 顶层健康状态规则为：启动未完成时 `STARTING`，依赖全部可用时 `UP`，启动完成但依赖异常时 `DEGRADED`。
- 当前 nginx 联调已验证 PostgreSQL 与 Redis 的真实 POST/GET 均可继续成功执行。
