# 方案归档索引

> 通过此文件快速查找历史方案

## 快速索引

| 时间戳 | 名称 | 类型 | 涉及模块 | 决策 | 结果 |
|--------|------|------|---------|------|------|
| 202603120228 | compose-split-legacy-autoscale | implementation | deploy-compose, nginx-gateway, autoscale-agent | compose-split-legacy-autoscale#D001,D002,D003 | ✅完成 |
| 202603111405 | compose-biz-service-autoscale | implementation | deploy-compose, nginx-gateway, biz-service, autoscale-agent | compose-biz-service-autoscale#D001,D002,D003 | ✅完成 |
| 202603110444 | biz-service-pg-redis-compose | - | - | - | ⏸未执行 |
| 202603110911 | ui-biz-nginx-compose-gateway | implementation | ui-service, nginx-gateway, deploy-compose | ui-biz-nginx-compose-gateway#D001,D002 | ✅完成 |
| 202603110540 | biz-service-real-readwrite | implementation | biz-service | biz-service-real-readwrite#D001 | ✅完成 |
| 202603110444 | pg-redis-compose-bootstrap | implementation | biz-service, deploy-compose | pg-redis-compose-bootstrap#D001 | ✅完成 |
| 202603110202 | springboot-biz-service-bootstrap | implementation | biz-service | springboot-biz-service-bootstrap#D001 | ✅完成 |
| 202603060947 | docker-compose-spring-stack-autoscaling | implementation | deploy-compose, ui-service, biz-service, monitoring-swarm | D001,D002,D004,D005 | ✅完成 |

## 按月归档

### 2026-03
- [202603120228_compose-split-legacy-autoscale](./2026-03/202603120228_compose-split-legacy-autoscale/) - 将当前单一 compose 拆分为 legacy 5 服务与 autoscale 6 服务双入口
- [202603111405_compose-biz-service-autoscale](./2026-03/202603111405_compose-biz-service-autoscale/) - 在当前 docker-compose 基线上落地 biz-service 自动扩缩容、nginx 动态 upstream 与 autoscale-agent
- [202603110444_biz-service-pg-redis-compose](./2026-03/202603110444_biz-service-pg-redis-compose/) - 历史遗留草稿方案包，已按未执行状态清理归档
- [202603110911_ui-biz-nginx-compose-gateway](./2026-03/202603110911_ui-biz-nginx-compose-gateway/) - 新增 ui-service 与 nginx，并扩展 docker-compose 为五服务编排
- [202603110540_biz-service-real-readwrite](./2026-03/202603110540_biz-service-real-readwrite/) - 为 PostgreSQL notes 与 Redis KV 新增真实写读接口并完成联调验证
- [202603110444_pg-redis-compose-bootstrap](./2026-03/202603110444_pg-redis-compose-bootstrap/) - 扩展 PostgreSQL / Redis 接入、依赖验证接口与 docker-compose 基线
- [202603110202_springboot-biz-service-bootstrap](./2026-03/202603110202_springboot-biz-service-bootstrap/) - 初始化 biz-service Spring Boot 样板、接口与 Docker 镜像
- [202603060947_docker-compose-spring-stack-autoscaling](./2026-03/202603060947_docker-compose-spring-stack-autoscaling/) - Docker Compose / Swarm 渐进式部署与自动扩缩容规划

## 结果状态说明
- ✅ 完成
- ⚠️ 部分完成
- ❌ 失败/中止
- ⏸ 未执行
- 🔄 已回滚
- 📄 概述
