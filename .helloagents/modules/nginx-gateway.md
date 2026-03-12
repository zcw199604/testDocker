# nginx-gateway

## 范围
- `nginx/nginx.conf`
- `nginx/generated/biz-service.upstream.inc`

## 当前事实
- 当前工作区已通过 `nginx/nginx.conf` 落地统一 HTTP 入口，监听 `80`。
- `http {}` 中静态定义 `ui_service_upstream`，并通过 `include /etc/nginx/generated/biz-service.upstream.inc;` 引入 `biz_service_upstream`。
- `/api/*` 当前代理目标为 `http://biz_service_upstream`，不再直接硬编码为单实例 `biz-service:8080`。
- `/ui-api/*` 与 `/` 继续代理到 `ui_service_upstream`。
- Nginx 继续暴露 `GET /nginx/health`，返回 `200 ok`。
- 当前 upstream include 文件由 `autoscale-agent` 生成，采用完整 upstream block 形式，而不是只包含 `server` 行片段。
- `autoscale-agent` 更新 upstream 时会先写临时文件，再执行 `nginx -t`，校验通过后执行 `nginx -s reload`；若校验失败则回滚旧文件。
- 当前 upstream 文件至少包含一个 seed backend；当 managed 副本健康通过后，会追加 managed IP backend；缩容时会先删除 managed backend，再触发 nginx reload。

## 运行关注点
- `biz_service_upstream` 当前采用 `least_conn` + `keepalive 32`。
- 代理配置会透传 `Host`、`X-Forwarded-For`、`X-Forwarded-Proto`、`X-Request-Id` 等基础头。
- 由于 managed 副本是通过 Docker SDK 直接创建的，nginx include 文件中的 managed 节点使用容器 IP，而 seed 节点使用 `biz-service:8080` 服务名。

## 已验证结果
- `GET http://127.0.0.1/nginx/health` 返回 `ok`。
- 扩容时 upstream 文件已从单 seed backend 更新为 seed + managed backend。
- 连续请求 `GET http://127.0.0.1/api/autoscale/metrics` 通过 nginx 可观察到 2 个不同 `instanceId`，证明 managed 副本已加入代理组并承接流量。
- 缩容时 upstream 文件已在容器 stop 前恢复为仅 seed backend。
