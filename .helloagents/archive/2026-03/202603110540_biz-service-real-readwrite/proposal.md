# 变更提案: biz-service-real-readwrite

## 元信息
```yaml
类型: 新功能
方案类型: implementation
优先级: P1
状态: 已完成
创建: 2026-03-11
更新: 2026-03-11 05:49:24 UTC
推荐方案: 在现有连接探测基线上，为 PostgreSQL 增加 notes 真写真读接口，为 Redis 增加 KV 真写真读接口，并沿用 compose 环境联调
```

---

## 1. 需求

### 背景
当前 `biz-service` 已经接入 PostgreSQL 与 Redis，也已具备健康检查和依赖探测能力，但目前仍停留在“连通性验证”层面。用户希望继续把它做成真实读写：不仅要能判断 PostgreSQL 与 Redis 是否可用，还要能够通过接口实际写入并读取 PostgreSQL 数据，以及实际写入并读取 Redis 数据。

### 目标
- 为 PostgreSQL 增加真实写入与读取接口。
- 为 Redis 增加真实写入与读取接口。
- 保持现有健康检查、依赖探测与 compose 联调能力可用。
- 在 compose 环境中完成真实写读联调验证。
- 同步知识库，沉淀真实读写能力与接口事实。

### 约束条件
```yaml
时间约束:
  - 基于当前 biz-service 增量扩展，不改写现有健康检查和依赖探测主路径。
性能约束:
  - 读写能力以最小演示模型为主，不引入不必要的复杂业务层。
兼容性约束:
  - 继续使用 Spring Boot 3.4.0、Java 21、Maven。
  - 继续沿用当前 Dockerfile 与根目录 docker-compose.yml。
业务约束:
  - PostgreSQL 与 Redis 都必须具备“真实写入 + 真实读取”。
  - 接口需直观、适合 curl 联调。
```

### 验收标准
- [x] PostgreSQL 已新增真实表结构与写入/读取接口。
- [x] Redis 已新增真实 key/value 写入/读取接口。
- [x] `docker-compose.yml` 环境中可完成 PostgreSQL 与 Redis 的真实写读验证。
- [x] 至少有一组自动化测试覆盖新增接口的核心返回结构。
- [x] 健康检查与依赖探测接口未被破坏。
- [x] 知识库与 CHANGELOG 已同步反映真实读写能力。

---

## 2. 方案

### 技术方案
采用“最小业务对象 + 最小 KV 存储”的方式实现真实读写：
- PostgreSQL：新增 `notes` 表，字段包含 `id`、`title`、`content`、`created_at`；通过 `JdbcTemplate` 实现插入、按 ID 查询、列表查询。
- Redis：新增简单 KV 接口，通过 `StringRedisTemplate` 实现按 key 写入 value、按 key 读取 value，并支持可选 TTL。
- 保持 `GET /api/health` 与 `GET /api/dependencies` 原有能力不变。
- 新增 `schema.sql`，在应用启动时初始化 PostgreSQL 演示表结构。
- 在 compose 环境中通过 curl 执行 PostgreSQL/Redis 实际写入与读取验证。

### 影响范围
```yaml
涉及模块:
  - biz-service: 新增 PostgreSQL notes 读写、Redis KV 读写、请求/响应模型、控制器、服务与 SQL 初始化
  - deploy-compose: 沿用当前根目录 compose 环境做联调验证
  - knowledge-base: 同步真实读写接口与使用方式
预计变更文件: 14~22
```

### 实施结果
- 已新增 `schema.sql` 与 `SPRING_SQL_INIT_MODE` 配置，在 compose 环境自动初始化 PostgreSQL `notes` 表。
- 已新增 PostgreSQL notes 的真实写入、按 ID 读取与列表读取接口。
- 已新增 Redis KV 的真实写入与读取接口，并支持 TTL。
- 已补充 WebMvc 测试，覆盖 PostgreSQL notes 与 Redis KV 接口的关键返回结构。
- 已通过 Docker build 与 compose 环境完成真实 POST/GET 联调验证。
- 已同步知识库、CHANGELOG 与归档索引。

### 风险评估
| 风险 | 等级 | 应对 |
|------|------|------|
| PostgreSQL 表结构未初始化导致写入失败 | 中 | 增加 `schema.sql` 并启用 Spring SQL 初始化 |
| Redis key 写入后被覆盖或 TTL 不一致 | 低 | 接口显式返回 key、value、ttlSeconds，保持行为可观测 |
| 新增接口影响现有测试与依赖探测 | 低 | 保持原接口不变，并补充新增接口测试 |

### 回滚方案
- 删除本轮新增的 PostgreSQL/Redis 真实读写接口与模型。
- 删除 `schema.sql` 和相关 SQL 初始化配置。
- 将知识库与 CHANGELOG 回退到变更前版本。
- 保留已有健康检查与依赖探测能力。

---

## 3. 技术设计

### API设计
#### POST /api/pg/notes
- **用途**: 向 PostgreSQL 写入一条 note。
- **请求**:
```json
{
  "title": "hello",
  "content": "world"
}
```
- **响应**: 返回 `id`、`title`、`content`、`createdAt`。

#### GET /api/pg/notes/{id}
- **用途**: 从 PostgreSQL 读取指定 note。
- **响应**: 返回 note 详情。

#### GET /api/pg/notes
- **用途**: 列出当前 notes。

#### POST /api/redis/kv
- **用途**: 向 Redis 写入一个 key/value。
- **请求**:
```json
{
  "key": "demo:key",
  "value": "hello",
  "ttlSeconds": 120
}
```
- **响应**: 返回 key、value、ttlSeconds、exists。

#### GET /api/redis/kv/{key}
- **用途**: 从 Redis 读取指定 key 的值。
- **响应**: 返回 key、value、exists、ttlSeconds。

### 数据模型
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | PostgreSQL note 主键 |
| title | String | note 标题 |
| content | String | note 内容 |
| createdAt | Instant | note 创建时间 |
| key | String | Redis 键 |
| value | String | Redis 值 |
| ttlSeconds | Long | Redis TTL 秒数 |

---

## 4. 核心场景

### 场景: PostgreSQL notes 真实写读
**模块**: biz-service
**条件**: compose 环境已启动且 PostgreSQL 可用
**行为**: 调用 `POST /api/pg/notes` 写入 note，再调用 `GET /api/pg/notes/{id}` 读取
**结果**: 返回数据库真实持久化的数据

### 场景: Redis KV 真实写读
**模块**: biz-service
**条件**: compose 环境已启动且 Redis 可用
**行为**: 调用 `POST /api/redis/kv` 写入 key/value，再调用 `GET /api/redis/kv/{key}` 读取
**结果**: 返回 Redis 中真实写入的数据与 TTL 信息

---

## 5. 技术决策

### biz-service-real-readwrite#D001: PostgreSQL 使用 JdbcTemplate 演示真实读写，而不是直接升级为 JPA 完整持久化层
**日期**: 2026-03-11
**状态**: ✅采纳
**背景**: 当前目标是“做成真实读写”，但未要求完整领域建模、Repository 体系或复杂 ORM 映射。
**选项分析**:
| 选项 | 优点 | 缺点 |
|------|------|------|
| A: 直接引入 Spring Data JPA | 更贴近正式业务项目 | 超出当前最小演示范围，引入额外实体与 ORM 复杂度 |
| B: 使用 JdbcTemplate 做最小真实读写 | 直接、轻量、便于理解和联调 | 抽象层较薄，后续扩展需再演进 |
**决策**: 选择方案 B
**理由**: 更符合“最小真实读写演示”的当前目标，同时与现有 JDBC 探测能力保持一致。
**影响**: 新增 `schema.sql`、JdbcTemplate 服务、note 相关 DTO 与控制器接口。
