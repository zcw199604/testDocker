# deploy-compose / 部署编排

## 范围
- `docker-compose.yml`

## 当前事实
- 当前工作区已存在一个根目录 `docker-compose.yml`，这是项目当前唯一真实可用的编排入口。
- Compose 当前包含 6 个服务：`postgres`、`redis`、`ui-service`、`biz-service`、`nginx`、`autoscale-agent`。
- PostgreSQL 基线镜像为 `postgres:16-alpine`，通过命名卷 `postgres_data` 持久化数据，并使用 `pg_isready` 健康检查。
- Redis 基线镜像为 `redis:7.4-alpine`，通过命名卷 `redis_data` 持久化数据，并使用 `redis-cli ping` 健康检查。
- `biz-service` 从 `./biz-service` 构建，当前不再暴露宿主机 `8080`，只通过 `expose: 8080` 供 Compose 网络访问。
- `biz-service` 带有 autoscale 相关 labels：服务端口、健康路径、指标路径、upstream 名称与 seed host。
- `biz-service` 当前额外注入 `AUTOSCALE_METRICS_WINDOW_DURATION=15s`，用于本地弹性演练。
- `ui-service` 仍从 `./ui-service` 构建，对外暴露 `8081:8080`，职责边界未纳入本轮自动扩缩容对象。
- `nginx` 挂载 `./nginx/nginx.conf` 与 `./nginx/generated`，其中 `generated` 目录用于接收 `autoscale-agent` 生成的 upstream include 文件。
- `autoscale-agent` 从 `./autoscale-agent` 构建，挂载 Docker Socket、只读 Compose 文件与 `nginx/generated` 目录，不对外暴露宿主机端口。
- `autoscale-agent` 当前通过环境变量配置 CPU / 内存阈值、业务指标阈值、poll interval、scale windows、cooldown 与 drain 窗口。

## 当前边界
- 当前部署基线仍是单机 `docker-compose`，未切换到 Swarm / Kubernetes。
- Compose 在当前实现中承担“服务模板与拓扑配置源”职责；副本增删与健康入池逻辑由 `autoscale-agent` 负责，而不是由 Compose 原生执行自动扩缩容策略。
- 数据层 `postgres`、`redis` 与交互层 `ui-service` 不参与本轮自动扩缩容。

## 当前端口与网络
- 对外端口：
  - `80 -> nginx`
  - `8081 -> ui-service`
  - `5432 -> postgres`
  - `6379 -> redis`
- 仅内网暴露：
  - `biz-service -> 8080/tcp`
  - `autoscale-agent -> 无对外端口`
- 当前代理规则：
  - `/api/* -> biz_service_upstream`
  - `/ui-api/* -> ui_service_upstream`
  - `/ -> ui_service_upstream`

## 已验证结果
- `docker-compose config --services` 已验证 6 服务定义完整。
- `docker-compose up -d --build` 已验证基础服务可拉起。
- `docker-compose ps` 已验证 `postgres`、`redis`、`biz-service`、`ui-service`、`nginx` healthy，`autoscale-agent` 持续运行。
