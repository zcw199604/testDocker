# biz-service

## 范围
- `biz-service/pom.xml`
- `biz-service/Dockerfile`
- `biz-service/src/main/java/`
- `biz-service/src/main/resources/application.yaml`
- `biz-service/src/test/java/`

## 当前事实
- `biz-service` 是一个基于 Spring Boot 3.4.0、Java 21、Maven 的业务服务，当前已接入 PostgreSQL 与 Redis，并具备真实读写能力。
- 服务继续提供：
  - `GET /api/health`
  - `GET /api/dependencies`
  - `GET /api/test`
  - `POST /api/pg/notes` / `GET /api/pg/notes/{id}` / `GET /api/pg/notes`
  - `POST /api/redis/kv` / `GET /api/redis/kv/{key}`
  - `GET /api/autoscale/metrics`
- `AutoscaleMetricsFilter` 当前统计 `/api/*` 业务请求，并排除 `/api/health`、`/api/dependencies`、`/api/autoscale/metrics`，避免健康探针污染扩缩容判断。
- `AutoscaleMetricsService` 当前维护 inflight 数、滚动窗口请求数、错误数、平均响应时间与 `requestRatePerSecond`。
- `AutoscaleMetricsResponse` 当前输出：`serviceName`、`instanceId`、`status`、`startupCompleted`、`inflightRequests`、`windowRequestCount`、`windowErrorCount`、`requestRatePerSecond`、`averageResponseTimeMs`、`windowStartedAt`、`timestamp`。
- `StartupState` 当前除了记录 `startedAt` / `readyAt` 外，还能表达 shutdown 开始时间与 `currentStatus()`，用于区分 `STARTING`、`READY`、`STOPPING`。
- `application.yaml` 当前包含：
  - `spring.application.instance-id`
  - `server.shutdown: graceful`
  - `spring.lifecycle.timeout-per-shutdown-phase`
  - `autoscale.metrics.window-duration`
- 在 `docker-compose.yml` 中，`biz-service` 当前恢复固定 `container_name: testdocker-biz-service-compose`，并向宿主机暴露 `8080:8080`。
- 在 `docker-compose.autoscale.yml` 中，`biz-service` 仅 `expose: 8080`，由 nginx 统一代理入口流量，并作为 `autoscale-agent` 的 seed 服务模板。

## 依赖关系
- 构建依赖：Spring Boot 3.4.0、Java 21、Maven。
- 运行依赖：PostgreSQL、Redis。
- 扩缩容依赖：`autoscale-agent` 通过 `/api/health` 与 `/api/autoscale/metrics` 观测实例状态。
- 网关依赖：两套 nginx 配置都保持 `/api/* -> biz_service_upstream`，但旧版 Compose 仍保留宿主机 `8080` 直连入口。

## 已验证结果
- `mvn test`（通过 Maven 容器执行）已通过。
- `GET http://127.0.0.1/api/health` 当前可返回服务状态与 PostgreSQL / Redis 依赖状态。
- `GET http://127.0.0.1/api/autoscale/metrics` 当前可返回实例级 autoscale 指标快照。
- 在自动扩容联调中，经 nginx 重复访问 `/api/autoscale/metrics` 已观测到 2 个不同 `instanceId`，证明多实例流量分发生效。
