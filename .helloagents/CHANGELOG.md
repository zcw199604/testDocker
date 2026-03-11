# CHANGELOG

## [0.5.0] - 2026-03-11

### 新增
- **[ui-service]**: 新增前端交互型 Spring Boot 服务，接入 PostgreSQL `ui_preferences` 与 Redis `ui:session:*`，并完成真实写读联调验证 — by zcw
  - 方案: [202603110911_ui-biz-nginx-compose-gateway](archive/2026-03/202603110911_ui-biz-nginx-compose-gateway/)
  - 决策: ui-biz-nginx-compose-gateway#D001(采用职责分层双直连，而不是让 ui-service 完全不连数据库)
- **[nginx-gateway]**: 新增 nginx 统一入口，完成 `/api/* -> biz-service`、`/ui-api/* 与 / -> ui-service` 的代理分流 — by zcw
  - 方案: [202603110911_ui-biz-nginx-compose-gateway](archive/2026-03/202603110911_ui-biz-nginx-compose-gateway/)
  - 决策: ui-biz-nginx-compose-gateway#D002(保持 biz-service `/api/*` 路由不变，并为 ui-service 新增 `/ui-api/*`)
- **[deploy-compose]**: 扩展根目录 docker-compose 基线为 5 服务编排，并验证 postgres、redis、biz-service、ui-service、nginx 全部 healthy — by zcw
  - 方案: [202603110911_ui-biz-nginx-compose-gateway](archive/2026-03/202603110911_ui-biz-nginx-compose-gateway/)
  - 决策: ui-biz-nginx-compose-gateway#D001,D002

## [0.4.0] - 2026-03-11

### 新增
- **[biz-service]**: 新增 PostgreSQL notes 与 Redis KV 真实读写接口，并在当前 docker-compose 基线上完成真实写读联调验证 — by zcw
  - 方案: [202603110540_biz-service-real-readwrite](archive/2026-03/202603110540_biz-service-real-readwrite/)
  - 决策: biz-service-real-readwrite#D001(PostgreSQL 使用 JdbcTemplate 实现最小真实读写，而不是直接升级为 JPA 完整持久化层)

## [0.3.0] - 2026-03-11

### 新增
- **[biz-service]**: 扩展 PostgreSQL / Redis 接入、新增依赖验证接口，并新增根目录 docker-compose 基线以同时启动 biz-service、PostgreSQL 与 Redis — by zcw
  - 方案: [202603110444_pg-redis-compose-bootstrap](archive/2026-03/202603110444_pg-redis-compose-bootstrap/)
  - 决策: pg-redis-compose-bootstrap#D001(采用 JDBC + Redis 轻量探测，而不是引入完整持久化业务层)

## [0.2.0] - 2026-03-11

### 新增
- **[biz-service]**: 初始化 Spring Boot 3.4.0 / Java 21 单服务样板，新增健康监测接口、测试接口、Actuator 基础观测与 Docker 多阶段镜像构建 — by zcw
  - 方案: [202603110202_springboot-biz-service-bootstrap](archive/2026-03/202603110202_springboot-biz-service-bootstrap/)
  - 决策: springboot-biz-service-bootstrap#D001(采用单服务 Spring Boot + 自定义健康接口 + Docker 多阶段构建)

## [0.1.0] - 2026-03-06

### 新增
- 初始化项目知识库，新增 `INDEX.md`、`context.md`、`modules/_index.md` 与 4 份模块文档，沉淀 Docker Compose + Docker Swarm 渐进式部署的历史规划。
- 方案包：[`docker-compose-spring-stack-autoscaling`](archive/2026-03/202603060947_docker-compose-spring-stack-autoscaling/proposal.md)（任务清单见 [`tasks.md`](archive/2026-03/202603060947_docker-compose-spring-stack-autoscaling/tasks.md)）。
- 决策引用：D001、D002、D004、D005。
- 作者：zcw。
