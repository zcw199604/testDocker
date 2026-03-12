# deploy-compose / 部署编排

## 范围
- `docker-compose.yml`
- `docker-compose.autoscale.yml`

## 当前事实
- 当前工作区存在两套 Compose 入口，它们是替代关系，不建议同时启动。
- `docker-compose.yml` 是旧版单机 5 服务入口，当前包含：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`。
- 旧版入口中：
  - `biz-service` 恢复 `container_name: testdocker-biz-service-compose` 与 `ports: 8080:8080`；
  - `nginx` 使用 `./nginx/nginx.legacy.conf:/etc/nginx/nginx.conf:ro`；
  - `/api/*` 通过静态 `biz_service_upstream` 代理到 `biz-service:8080`。
- `docker-compose.autoscale.yml` 是本地自动扩缩容 6 服务入口，在上述 5 个服务基础上增加 `autoscale-agent`。
- autoscale 入口中：
  - `biz-service` 仅 `expose: 8080`，不再向宿主机映射 `8080`；
  - `biz-service` 带有 autoscale labels，并注入 `AUTOSCALE_METRICS_WINDOW_DURATION=15s`；
  - `nginx` 使用 `./nginx/nginx.conf` 与 `./nginx/generated`；
  - `autoscale-agent` 继续把 Compose 当作模板源，但当前宿主挂载文件是 `./docker-compose.autoscale.yml`，容器内路径仍为 `/workspace/docker-compose.yml`。
- 两套入口都保留 `postgres:5432`、`redis:6379`、`ui-service:8081` 与 `nginx:80` 的宿主机端口映射。

## 当前边界
- 当前部署基线仍是单机 `docker-compose`，未切换到 Swarm / Kubernetes。
- `autoscale-agent` 只存在于 `docker-compose.autoscale.yml`；旧版 `docker-compose.yml` 不包含自动扩缩容控制层。
- 两套 Compose 共用 `name: testdocker`，且复用相同服务名、端口与部分固定容器名，不能视为可并行共存的两套独立环境。

## 当前端口与网络
- `docker-compose.yml`：对外暴露 `80`、`8080`、`8081`、`5432`、`6379`。
- `docker-compose.autoscale.yml`：对外暴露 `80`、`8081`、`5432`、`6379`；`biz-service` 仅在容器网络内暴露 `8080/tcp`；`autoscale-agent` 无对外端口。
- 两套入口下的网关路由口径保持一致：
  - `/api/* -> biz_service_upstream`
  - `/ui-api/* -> ui_service_upstream`
  - `/ -> ui_service_upstream`

## 已验证结果
- `docker-compose -f docker-compose.yml config --services` 当前输出 5 个服务：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`。
- `docker-compose -f docker-compose.autoscale.yml config --services` 当前输出 6 个服务：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`、`autoscale-agent`。
