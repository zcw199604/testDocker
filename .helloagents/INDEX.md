# 项目知识库索引

## 当前状态
- 当前代码树已实际落地 2 个 Spring Boot 服务：`biz-service/` 与 `ui-service/`。
- 当前存在两套互斥的 Compose 入口：
  - `docker-compose.yml`：旧版单机 5 服务（`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`）。
  - `docker-compose.autoscale.yml`：本地自动扩缩容 6 服务，在上述基础上增加 `autoscale-agent`。
- 两套 Compose 共用 `name: testdocker`，并复用相同服务名、端口与部分固定容器名，属于替代关系，不建议同时启动。
- 网关也分为两套配置：
  - `nginx/nginx.legacy.conf`：旧版静态 upstream，`/api/*` 走静态 `biz_service_upstream`。
  - `nginx/nginx.conf` + `nginx/generated/biz-service.upstream.inc`：autoscale 版生成式 upstream，`/api/*` 仍走 `biz_service_upstream`。
- `autoscale-agent` 已真实落地，仍以 Compose 作为 `biz-service` 模板源；当前宿主模板文件为 `docker-compose.autoscale.yml`，容器内路径仍映射为 `/workspace/docker-compose.yml`。
- 当前工作区仍未落地 Swarm / Prometheus / Kubernetes；相关内容只保留为历史规划参考。

## 快速导航
- [项目上下文](context.md)
- [变更记录](CHANGELOG.md)
- [模块索引](modules/_index.md)
- [方案归档](archive/_index.md)

## 当前配置校验
- `docker-compose -f docker-compose.yml config --services` 当前输出 5 个服务：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`。
- `docker-compose -f docker-compose.autoscale.yml config --services` 当前输出 6 个服务：`postgres`、`redis`、`biz-service`、`ui-service`、`nginx`、`autoscale-agent`。
- `biz-service` 在旧版 Compose 中对宿主暴露 `8080:8080`；在 autoscale Compose 中仅 `expose: 8080`，由 nginx 统一代理入口流量。

## 当前模块
- [biz-service](modules/biz-service.md)
- [ui-service](modules/ui-service.md)
- [nginx-gateway](modules/nginx-gateway.md)
- [autoscale-agent](modules/autoscale-agent.md)
- [deploy-compose / 部署编排](modules/deploy-compose.md)
- [monitoring-swarm / 监控与弹性（历史规划）](modules/monitoring-swarm.md)
