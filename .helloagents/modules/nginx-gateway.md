# nginx-gateway

## 范围
- `nginx/nginx.legacy.conf`
- `nginx/nginx.conf`
- `nginx/generated/biz-service.upstream.inc`

## 当前事实
- 当前网关存在两套与 Compose 入口配套的 Nginx 配置，属于替代关系。
- `nginx/nginx.legacy.conf` 由 `docker-compose.yml` 使用：
  - 静态定义 `ui_service_upstream` 与 `biz_service_upstream`；
  - `/api/*` 代理到 `http://biz_service_upstream`，其后端固定为 `biz-service:8080`；
  - `/ui-api/*` 与 `/` 代理到 `ui_service_upstream`。
- `nginx/nginx.conf` 由 `docker-compose.autoscale.yml` 使用：
  - 静态定义 `ui_service_upstream`；
  - 通过 `include /etc/nginx/generated/biz-service.upstream.inc;` 引入生成式 `biz_service_upstream`；
  - `/api/*` 仍代理到 `http://biz_service_upstream`。
- 两套配置都暴露 `GET /nginx/health`，返回 `200 ok`。
- autoscale 入口下，`nginx/generated/biz-service.upstream.inc` 由 `autoscale-agent` 生成，文件内容是完整 upstream block，不是单独的 `server` 行片段。
- `autoscale-agent` 更新 upstream 时会先写临时文件，再执行 `nginx -t`；校验通过后执行 `nginx -s reload`，失败则回滚旧文件。

## 运行关注点
- 旧版配置对应单 seed 静态代理；autoscale 配置对应 seed + managed 副本池。
- autoscale 配置中，seed 节点使用 `biz-service:8080` 服务名；managed 节点通过生成文件写入容器 IP。
- 使用旧版 Compose 时应配套 `nginx/nginx.legacy.conf`；使用 autoscale Compose 时应配套 `nginx/nginx.conf` 与 `nginx/generated/`，不要混用。

## 已验证结果
- `GET http://127.0.0.1/nginx/health` 返回 `ok`。
- autoscale 联调中，upstream 文件已从单 seed backend 更新为 seed + managed backend。
- 连续请求 `GET http://127.0.0.1/api/autoscale/metrics` 通过 nginx 可观察到 2 个不同 `instanceId`，证明 managed 副本已加入代理组并承接流量。
- 缩容时 upstream 文件已在容器 stop 前恢复为仅 seed backend。
