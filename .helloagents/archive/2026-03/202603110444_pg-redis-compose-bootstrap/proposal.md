# 变更提案: pg-redis-compose-bootstrap

## 元信息
```yaml
类型: 新功能
方案类型: implementation
优先级: P1
状态: 已完成
创建: 2026-03-11
更新: 2026-03-11 05:00:49 UTC
推荐方案: 在现有 biz-service 上扩展 PostgreSQL/Redis 连接探测，并新增根目录 docker-compose.yml 统一启动三项服务
```

---

## 1. 需求

### 背景
当前仓库已具备一个最小可运行的 `biz-service` Spring Boot 服务，提供了基础健康接口与测试接口，但还没有接入 PostgreSQL 与 Redis，也没有可直接拉起应用与依赖服务的实际 Docker Compose 文件。用户希望在当前样板基础上继续演进：让应用能够实际连接 PostgreSQL 与 Redis，提供验证这两个依赖是否可用的接口，并在启动编排时一并拉起应用、PostgreSQL 与 Redis。

### 目标
- 为 `biz-service` 增加 PostgreSQL 与 Redis 连接能力。
- 新增或扩展接口，用于验证 PostgreSQL 与 Redis 是否可用，并返回清晰的连通性结果。
- 扩展 `GET /api/health`，使其同时体现应用自身启动状态与 PostgreSQL / Redis 的可用状态。
- 新增根目录 `docker-compose.yml`，使 `biz-service`、`postgres`、`redis` 可以一并启动。
- 完成容器级联调验证，确保 compose 启动后服务可访问并可正确报告依赖状态。

### 约束条件
```yaml
时间约束:
  - 基于现有 biz-service 最小样板做增量扩展，不重做项目结构。
性能约束:
  - 健康与依赖探测需轻量实现，避免为验证功能引入过重业务层。
兼容性约束:
  - 继续沿用 Spring Boot 3.4.0、Java 21、Maven。
  - 当前工作区没有现成 compose 文件，因此本轮以新增根目录 `docker-compose.yml` 为准。
业务约束:
  - 必须能明确判断 PostgreSQL 与 Redis 是否可用。
  - 启动 compose 时应同时拉起 `biz-service`、`postgres`、`redis`。
```

### 验收标准
- [x] `biz-service` 已新增 PostgreSQL 与 Redis 相关依赖与配置。
- [x] `GET /api/health` 返回应用、PostgreSQL、Redis 的综合状态。
- [x] 存在一个专门接口用于验证 PostgreSQL 与 Redis 是否可用，并返回可读详情。
- [x] 根目录新增 `docker-compose.yml`，可同时启动 `biz-service`、`postgres`、`redis`。
- [x] Docker 构建与 compose 联调成功，接口能返回 PostgreSQL / Redis 为可用状态。
- [x] 知识库同步反映本轮新增的依赖与编排事实。

---

## 2. 方案

### 技术方案
采用“轻量连接探测 + 容器统一编排”的实现方式：
- 在 `biz-service` 中增加 JDBC、PostgreSQL Driver 与 Spring Data Redis 依赖；
- 通过一个 `DependencyStatusService` 对 PostgreSQL 执行 `SELECT current_database()` / 元信息读取，对 Redis 执行 `PING`；
- 新增 `GET /api/dependencies`，专门返回 PostgreSQL 与 Redis 的可用状态、错误信息与基础元数据；
- 扩展 `GET /api/health` 返回综合状态与依赖详情；
- 在根目录新增 `docker-compose.yml`，编排 `postgres`、`redis`、`biz-service`，并通过健康检查与 `depends_on` 控制启动顺序。

### 影响范围
```yaml
涉及模块:
  - biz-service: 新增 PostgreSQL / Redis 连接、依赖状态探测与接口扩展
  - deploy-compose: 首次新增实际 `docker-compose.yml` 现实文件
  - knowledge-base: 同步更新当前项目状态与编排事实
预计变更文件: 12~18
```

### 实施步骤
- 扩充 `pom.xml` 与 `application.yaml`，增加 PostgreSQL / Redis 配置。
- 实现依赖状态模型与探测服务，分别验证 PostgreSQL 与 Redis 连通性。
- 扩展健康接口并新增依赖验证接口。
- 更新控制器测试，覆盖依赖验证返回结构。
- 新增根目录 `docker-compose.yml`，编排应用、PostgreSQL 与 Redis。
- 通过 Docker build 与 Docker Compose 启动联调验证接口。
- 同步知识库、更新 CHANGELOG 并归档方案包。

### 实施结果摘要
- 已在 `biz-service/pom.xml` 中增加 JDBC、PostgreSQL Driver 与 Spring Data Redis 依赖。
- 已在 `application.yaml` 中补充 PostgreSQL / Redis 配置，以及允许依赖异常时应用仍可启动的连接参数。
- 已新增 `GET /api/dependencies`，并扩展 `GET /api/health` 返回 PostgreSQL / Redis 的状态详情。
- 已新增根目录 `docker-compose.yml`，可同时启动 `biz-service`、`postgres:16-alpine`、`redis:7.4-alpine`。
- 已通过 `docker build` 构建镜像，并通过 `docker-compose up -d --build` 完成联调。
- 已验证 `biz-service`、`postgres`、`redis` 三个容器均为 `healthy`，接口返回 PostgreSQL 与 Redis 为可用状态。

### 风险评估
| 风险 | 等级 | 应对 |
|------|------|------|
| PostgreSQL/Redis 未就绪导致应用启动阶段探测失败 | 中 | 在 compose 中为依赖添加健康检查，并对应用使用 `depends_on: condition: service_healthy` |
| 外部依赖不可用导致 Spring Boot 启动失败 | 中 | 通过配置降低初始化失败阻断，让应用可启动并通过接口报告依赖异常 |
| 现有知识库仍包含旧的历史规划叙述 | 中 | 以当前代码与新 compose 文件为准，更新 INDEX/context/modules/CHANGELOG |

### 回滚方案
- 删除本轮新增的 `docker-compose.yml`。
- 将 `biz-service` 中新增的 PostgreSQL / Redis 依赖与接口改动回退到上一版。
- 将 `.helloagents/INDEX.md`、`context.md`、`modules/_index.md`、`modules/biz-service.md`、`modules/deploy-compose.md`、`CHANGELOG.md` 回退到变更前版本。

---

## 3. 技术设计

### API 设计
#### GET /api/health
- **用途**: 返回应用整体状态、启动完成状态与 PostgreSQL / Redis 依赖状态。
- **响应**:
```json
{
  "serviceName": "biz-service",
  "status": "UP",
  "startupCompleted": true,
  "dependencies": {
    "postgres": {
      "available": true
    },
    "redis": {
      "available": true
    }
  }
}
```

#### GET /api/dependencies
- **用途**: 专门验证 PostgreSQL 与 Redis 是否可用。
- **响应**:
```json
{
  "overallStatus": "UP",
  "postgres": {
    "available": true,
    "database": "bizdb"
  },
  "redis": {
    "available": true,
    "ping": "PONG"
  }
}
```

### 配置设计
- PostgreSQL 通过 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` 注入。
- Redis 通过 `SPRING_DATA_REDIS_HOST`、`SPRING_DATA_REDIS_PORT` 注入。
- Compose 默认使用：`postgres:16-alpine`、`redis:7.4-alpine`、本地构建 `biz-service`。

---

## 4. 核心场景

### 场景: compose 一键启动应用与依赖
**模块**: deploy-compose
**条件**: 执行 `docker-compose up -d --build`
**行为**: 启动 PostgreSQL、Redis 与 biz-service，并等待依赖健康后再启动应用
**结果**: 访问 `GET /api/health` 与 `GET /api/dependencies` 可看到 PostgreSQL / Redis 可用

### 场景: 接口验证依赖状态
**模块**: biz-service
**条件**: 应用已启动
**行为**: 调用依赖验证接口执行 PostgreSQL 查询和 Redis ping
**结果**: 返回结构化状态与错误信息，便于快速判断依赖是否联通

---

## 5. 技术决策

### pg-redis-compose-bootstrap#D001: 采用 JDBC + Redis 轻量探测，而不是引入完整持久化业务层
**日期**: 2026-03-11
**状态**: ✅采纳
**背景**: 当前需求聚焦“接入 PostgreSQL / Redis 并验证是否可用”，并未要求完整数据模型或仓储层。
**选项分析**:
| 选项 | 优点 | 缺点 |
|------|------|------|
| A: 引入 JPA、实体、Repository | 便于后续业务扩展 | 为当前纯验证需求增加不必要复杂度 |
| B: 使用 JDBC + Redis 轻量探测 | 改动小、验证直接、适合当前需求 | 暂不提供完整持久化抽象 |
**决策**: 选择方案 B
**理由**: 当前目标是最小增量扩展依赖接入与健康验证，轻量探测更符合需求边界。
**影响**: `biz-service` 的依赖管理、配置文件、依赖验证服务与接口设计。
