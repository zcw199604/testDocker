# 项目知识库索引

## 当前状态
- 当前代码树已实际落地两个 Spring Boot 服务：`biz-service/` 与 `ui-service/`，两者均基于 Spring Boot 3.4.0、Java 21、Maven。
- `biz-service` 提供 `/api/*` 业务数据处理接口，并已接入 PostgreSQL / Redis 真实读写能力。
- `ui-service` 提供 `/` 与 `/ui-api/*` 前端交互接口，并已接入 PostgreSQL / Redis 真实读写能力。
- 根目录 `docker-compose.yml` 当前可同时启动 `postgres`、`redis`、`biz-service`、`ui-service`、`nginx` 五个服务，并已验证五者均 `healthy`。
- `nginx` 已作为统一入口落地，当前路由规则为：`/api/* -> biz-service`，`/ui-api/*` 与 `/ -> ui-service`。
- 知识库中关于 Swarm、监控与自动扩缩容的内容仍保留为历史规划参考，当前工作区并无对应代码目录，不应视为已落地实现。

## 文档入口
- [项目上下文](context.md)：整体结构、当前真实代码边界与历史规划说明。
- [变更记录](CHANGELOG.md)：知识库同步历史与关联方案包。
- [模块索引](modules/_index.md)：按模块拆分的事实文档。

## 模块清单
- [biz-service](modules/biz-service.md) — 当前已落地
- [ui-service](modules/ui-service.md) — 当前已落地
- [nginx-gateway](modules/nginx-gateway.md) — 当前已落地
- [deploy-compose / 部署编排](modules/deploy-compose.md) — 当前已落地
- monitoring-swarm / 监控与弹性 — 历史规划（当前代码缺失）

## 关联方案包
- [biz-service-real-readwrite / proposal](archive/2026-03/202603110540_biz-service-real-readwrite/proposal.md)
- [biz-service-real-readwrite / tasks](archive/2026-03/202603110540_biz-service-real-readwrite/tasks.md)
- [pg-redis-compose-bootstrap / proposal](archive/2026-03/202603110444_pg-redis-compose-bootstrap/proposal.md)
- [pg-redis-compose-bootstrap / tasks](archive/2026-03/202603110444_pg-redis-compose-bootstrap/tasks.md)
- [springboot-biz-service-bootstrap / proposal](archive/2026-03/202603110202_springboot-biz-service-bootstrap/proposal.md)
- [springboot-biz-service-bootstrap / tasks](archive/2026-03/202603110202_springboot-biz-service-bootstrap/tasks.md)
- [docker-compose-spring-stack-autoscaling / proposal](archive/2026-03/202603060947_docker-compose-spring-stack-autoscaling/proposal.md)
- [docker-compose-spring-stack-autoscaling / tasks](archive/2026-03/202603060947_docker-compose-spring-stack-autoscaling/tasks.md)
