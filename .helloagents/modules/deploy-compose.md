# deploy-compose / 部署编排

## 范围
- `docker-compose.yml`

## 当前事实
- 当前工作区已存在一个根目录 `docker-compose.yml`，这是项目当前真实可用的 Compose 编排基线。
- Compose 当前包含 5 个核心服务：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`。
- PostgreSQL 基线镜像为 `postgres:16-alpine`，默认使用 `bizdb` / `bizuser`，通过命名卷 `postgres_data` 持久化数据，并使用 `pg_isready` 进行健康检查。
- Redis 基线镜像为 `redis:7.4-alpine`，以 `redis-server --appendonly yes` 启动，通过命名卷 `redis_data` 持久化数据，并使用 `redis-cli ping` 进行健康检查。
- `biz-service` 从本地 `./biz-service` Dockerfile 构建，暴露宿主机端口 `8080`，并通过环境变量连接 PostgreSQL 与 Redis。
- `ui-service` 从本地 `./ui-service` Dockerfile 构建，暴露宿主机端口 `8081`，并通过环境变量连接 PostgreSQL 与 Redis。
- Compose 为 `biz-service` 与 `ui-service` 都注入 `SPRING_SQL_INIT_MODE=always`，分别用于自动初始化 `notes` 与 `ui_preferences` 相关表结构。
- `biz-service` 与 `ui-service` 都依赖 `postgres` 与 `redis` 的健康状态；只有当两者健康后，应用容器才会启动。
- `nginx` 使用 `nginx:alpine` 镜像，并依赖 `biz-service` 与 `ui-service` 的健康状态后再启动。
- `nginx` 通过只读挂载 `./nginx/nginx.conf` 作为运行配置，并通过 `GET /nginx/health` 做容器健康检查。
- 当前联调验证结果显示 `postgres`、`redis`、`biz-service`、`ui-service`、`nginx` 均可达到 `healthy` 状态。

## 当前边界
- 当前 Compose 基线聚焦本地开发与最小联调场景，已经覆盖 UI 服务、业务服务、数据库、缓存与统一入口网关。
- 历史知识库中关于 Traefik、Swarm、Prometheus 或 autoscaler 等更大规模编排的描述，当前仅保留为规划参考，不是当前代码事实。
- 如后续继续扩展更多服务，应以现有根目录 `docker-compose.yml` 为真实基线继续演进。

## 当前路由与端口
- 对外端口：
  - `80 -> nginx`
  - `8080 -> biz-service`
  - `8081 -> ui-service`
  - `5432 -> postgres`
  - `6379 -> redis`
- 当前代理规则：
  - `/api/* -> biz-service:8080`
  - `/ui-api/* -> ui-service:8080`
  - `/ -> ui-service:8080`
