# 项目知识库索引

## 当前状态
- 当前代码树已实际落地 2 个 Spring Boot 服务：`biz-service/` 与 `ui-service/`。
- `biz-service` 现在额外具备 autoscale 指标采集、graceful shutdown 与生命周期状态输出能力。
- 根目录 `docker-compose.yml` 当前可同时启动 6 个服务：`postgres`、`redis`、`ui-service`、`biz-service`、`nginx`、`autoscale-agent`。
- `nginx` 已从静态单实例代理演进为动态 upstream 模式：`/api/* -> biz_service_upstream`，`/ui-api/*` 与 `/ -> ui_service_upstream`。
- `autoscale-agent` 已真实落地，负责基于业务指标 + 容器 CPU / 内存执行 `biz-service` 自动扩缩容，并在健康通过后入池、缩容前先摘流。
- 当前工作区仍未落地 Swarm / Prometheus / Kubernetes；相关内容只保留为历史规划参考。

## 快速导航
- [项目上下文](context.md)
- [变更记录](CHANGELOG.md)
- [模块索引](modules/_index.md)
- [方案归档](archive/_index.md)

## 最新已验证能力
- `GET /api/health`、`GET /api/autoscale/metrics`、`GET /nginx/health` 可通过 nginx 入口访问。
- `autoscale-agent` 已完成至少两轮扩容 + 缩容联调，验证点包括：
  - 扩容副本在健康通过前不加入 nginx upstream；
  - 经 nginx 访问 `/api/autoscale/metrics` 可观察到多个 `instanceId`；
  - 缩容前会先从 upstream 摘除目标副本，再等待 drain 并删除容器。
- 当前实现口径是“Compose 配置源协调型”，即 Compose 负责服务模板与拓扑定义，`autoscale-agent` 通过 Docker SDK 执行副本生命周期控制。

## 当前模块
- [biz-service](modules/biz-service.md)
- [ui-service](modules/ui-service.md)
- [nginx-gateway](modules/nginx-gateway.md)
- [autoscale-agent](modules/autoscale-agent.md)
- [deploy-compose / 部署编排](modules/deploy-compose.md)
- [monitoring-swarm / 监控与弹性（历史规划）](modules/monitoring-swarm.md)
