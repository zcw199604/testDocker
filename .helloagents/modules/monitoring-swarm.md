# monitoring-swarm / 监控与弹性

## 范围
- `deploy/swarm/docker-stack.yml`
- `deploy/swarm/mapping.md`
- `deploy/monitoring/prometheus/prometheus.yml`
- `deploy/monitoring/autoscaler/`
- `deploy/scripts/ops/deploy-swarm.sh`

## 当前事实
- `deploy/swarm/docker-stack.yml` 已把 Compose 基线扩展为 Swarm Stack，并新增 `prometheus` 与 `autoscaler` 两个增强服务。
- Swarm 侧为 `traefik`、`ui-service`、`biz-service` 保留了与 Compose 基本一致的路由标签与服务命名。
- Stack 使用 `edge`、`app`、`data`、`monitor` 四个 overlay 网络，并为 PostgreSQL / Redis 准备 secrets、为 Prometheus / autoscaler 准备 configs。
- `deploy/swarm/mapping.md` 已明确节点标签建议：manager 承载 Traefik/Prometheus/autoscaler，data 承载 PostgreSQL/Redis，app 承载两个 Spring Boot 服务。
- `deploy/monitoring/prometheus/prometheus.yml` 当前抓取 `prometheus`、`traefik`、`ui-service`、`biz-service`，并预留 `cadvisor`、`node-exporter` 目标。
- `deploy/scripts/ops/deploy-swarm.sh` 已提供 `docker stack deploy` 入口。

## autoscaler 实现事实
- autoscaler 镜像基于 `python:3.12-alpine`。
- 依赖清单包含 `docker`、`requests`、`redis`、`PyYAML`。
- `config.yaml` 采用 `dry_run: true`、`poll_interval_seconds: 30`、`metrics_missing_freeze: true` 等保护参数。
- 扩缩容规则仅覆盖 `ui-service` 与 `biz-service`，并具备 `min_replicas`、`max_replicas`、阈值、冷却时间、单次步进配置。
- 代码中已实现 Prometheus HTTP 查询、Docker Swarm service 读写、Redis 分布式锁、规则计算与单元测试。

## 当前边界
- 监控与弹性资产已形成可继续开发的增强骨架，但并非完整生产闭环：Prometheus 配置里提到的 `cadvisor`、`node-exporter` 尚未在 Stack 文件中一并定义。
- 数据层仍按 D003 保持单实例稳定部署，不属于自动扩缩容对象。
- 当前 autoscaler 默认 dry-run，更偏向“可验证骨架”而非直接执行缩放的生产模式。
