# nginx-gateway

## 范围
- `nginx/nginx.conf`

## 当前事实
- 当前工作区已通过 `nginx/nginx.conf` 落地一个基于 Nginx 的统一 HTTP 入口。
- Nginx 当前监听 `80`，在 compose 中通过 `nginx:alpine` 镜像启动，并挂载本地 `nginx.conf` 作为只读配置。
- 当前路由规则为：
  - `/api/* -> biz-service:8080`
  - `/ui-api/* -> ui-service:8080`
  - `/ -> ui-service:8080`
- Nginx 当前提供 `GET /nginx/health` 作为自身健康检查接口，返回 `200 ok`。
- 代理配置会透传以下基础头：`Host`、`X-Forwarded-For`、`X-Forwarded-Proto`、`X-Request-Id`。
- 当前联调验证中，nginx 已成功代理 ui-service 的 `/` 与 `/ui-api/*` 路由，也已成功代理 biz-service 的 `/api/*` 路由。

## 依赖关系
- 运行依赖：`biz-service`、`ui-service`。
- 编排依赖：在 `docker-compose.yml` 中等待 `biz-service` 与 `ui-service` 健康后启动。

## 运行关注点
- 当前配置以最小本地联调为目标，尚未增加 TLS、限流、鉴权或静态资源缓存等增强能力。
- 若未来继续扩展更多路径，应保持 `/api/*` 与 `/ui-api/*` 的职责边界清晰，避免把前端交互接口继续塞回业务路径。
