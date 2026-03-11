# 变更提案: springboot-biz-service-bootstrap

## 元信息
```yaml
类型: 新功能
方案类型: implementation
优先级: P1
状态: 已完成
创建: 2026-03-11
更新: 2026-03-11 02:13:35 UTC
推荐方案: 单服务 Spring Boot + 自定义健康监测 + Docker 多阶段构建
```

---

## 1. 需求

### 背景
当前仓库只有知识库文件，尚未存在可运行的 Java 服务代码。用户希望在项目中补齐一个可直接构建为 Docker 镜像的 Spring Boot 服务，并提供两个最小可验证接口：一个健康监测接口，用于确认服务状态与是否成功启动；一个测试接口，用于快速验证 HTTP 服务可用性。

同时，现有知识库中存在早期规划痕迹，与当前代码事实不一致。本次实施应以代码为准，先落地最小可运行的 `biz-service` 服务，再同步更新知识库。

### 目标
- 初始化 `biz-service/` Spring Boot Maven 项目，技术栈固定为 Spring Boot 3.4.0 + Java 21。
- 提供 `GET /api/health` 接口，返回服务状态、启动完成标志与启动时间信息。
- 提供 `GET /api/test` 接口，返回可读的测试响应。
- 提供可构建运行的 Dockerfile，使服务能够打包为单镜像并监听 `8080`。
- 提供最小自动化测试与容器级验证，确保镜像可启动、接口可访问。

### 约束条件
```yaml
时间约束:
  - 以最小可运行交付为目标，不引入数据库、缓存、消息队列等额外依赖。
性能约束:
  - 服务保持无状态，接口响应轻量，适合作为后续容器化基础镜像样板。
兼容性约束:
  - 固定使用 Maven、Spring Boot 3.4.0、Java 21。
  - 镜像需在容器内完成构建，不依赖本机预装 Java 或 Maven。
业务约束:
  - 健康接口必须体现“服务是否已成功启动”。
  - 测试接口由实现方自由设计，但必须可直观看出服务正常响应。
```

### 验收标准
- [x] `biz-service/` 下存在可构建的 Spring Boot Maven 工程，执行容器化构建后能产出镜像。
- [x] `GET /api/health` 返回 `status`、`startupCompleted`、时间戳等健康信息，并在服务启动完成后显示成功状态。
- [x] `GET /api/test` 能稳定返回 200 响应，并包含可读的测试字段。
- [x] Docker 镜像运行后监听 `8080`，可通过 HTTP 调用上述两个接口。
- [x] 至少有一组自动化测试覆盖核心接口。
- [x] 知识库已更新为与当前实际代码一致，不再把未落地内容当作已实现事实。

---

## 2. 方案

### 技术方案
采用单模块 Spring Boot 服务方案：
- 使用 `spring-boot-starter-web` 提供 REST 接口；
- 使用 `spring-boot-starter-actuator` 暴露标准健康/信息端点；
- 通过 `ApplicationReadyEvent` 记录启动完成状态，再由自定义健康接口返回用户可读的启动信息；
- 使用 `spring-boot-starter-test` + MockMvc 编写最小接口测试；
- 使用多阶段 Dockerfile，在 Maven 构建镜像阶段完成打包，在 JRE 运行镜像阶段暴露 `8080` 并附加健康检查。

### 影响范围
```yaml
涉及模块:
  - biz-service: 新增 Spring Boot 服务、接口与测试
  - knowledge-base: 同步修正项目上下文与 biz-service 模块事实
预计变更文件: 12+
```

### 实施结果
- 已创建 `biz-service` Maven 工程骨架与基础配置。
- 已实现启动状态跟踪、健康接口、测试接口与 Actuator 健康指标同步。
- 已补充 MockMvc 测试，覆盖健康与测试接口。
- 已编写 Dockerfile 与 `.dockerignore`，支持容器内构建与运行。
- 已通过 Docker 镜像构建、启动、接口验证与容器健康检查。
- 已按代码事实同步知识库与变更记录。

### 风险评估
| 风险 | 等级 | 应对 |
|------|------|------|
| 宿主机未安装 Java/Maven，导致本地无法直接编译 | 中 | 通过 Docker 多阶段构建完成 Maven 打包与测试 |
| 现有知识库与代码现状不一致 | 中 | 以代码为准更新 context/INDEX/modules 文档 |
| Alpine 运行镜像健康检查命令兼容性问题 | 低 | 使用基础 BusyBox 提供的 `wget` 与 `grep` 进行健康检查 |

### 回滚方案
- 删除 `biz-service/` 目录及本次新增的方案包。
- 将 `.helloagents/INDEX.md`、`context.md`、`modules/_index.md`、`modules/biz-service.md`、`CHANGELOG.md` 回退到变更前版本。
- 如镜像已构建，删除本地测试镜像与容器即可恢复到变更前状态。

---

## 3. 技术设计

### API 设计
#### GET /api/health
- **用途**: 对外提供人类可读的健康监测信息。
- **响应**:
```json
{
  "serviceName": "biz-service",
  "status": "UP",
  "startupCompleted": true,
  "startedAt": "2026-03-11T02:13:02Z",
  "readyAt": "2026-03-11T02:13:02Z",
  "uptimeSeconds": 1,
  "timestamp": "2026-03-11T02:13:03Z"
}
```

#### GET /api/test
- **用途**: 作为简单联调与镜像验证接口。
- **请求参数**: `name`（可选）
- **响应**:
```json
{
  "endpoint": "test",
  "message": "Hello, Codex! biz-service is responding normally.",
  "requestedName": "Codex",
  "timestamp": "2026-03-11T02:13:03Z"
}
```

### 决策记录
### springboot-biz-service-bootstrap#D001: 采用自定义健康接口而非仅依赖 Actuator 默认响应
**日期**: 2026-03-11
**状态**: ✅采纳
**背景**: 用户明确需要接口直接体现“服务状态与是否成功启动”，默认 Actuator JSON 不足以表达自定义业务字段。
**选项分析**:
| 选项 | 优点 | 缺点 |
|------|------|------|
| A: 仅使用 `/actuator/health` | 标准化、实现快 | 无法直接返回启动完成语义字段 |
| B: 增加 `/api/health` 并保留 Actuator | 满足用户需求，便于 Docker 健康检查 | 需要额外少量代码 |
**决策**: 选择方案 B
**理由**: 同时兼顾用户可读性与 Spring Boot 标准观测能力。
**影响**: `biz-service` 控制器、启动状态组件、Docker 健康检查路径。
