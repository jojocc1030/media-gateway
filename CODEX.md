# CODEX.md

## 项目概述

`media-gateway` 是**视频数据网关**的通用脚手架工程，技术栈与 magpie 完全一致，单模块 Spring Boot 组织。

工程**不包含具体视频、流媒体、台账业务**，只提供网关类系统通用的工程能力：统一响应、异常、会话鉴权与角色、
持久层规范、幂等、链路日志、外部 HTTP 调用封装、示例业务模块与测试规范。

目标系统能力（在后继业务开发中落地）：ZLMediaKit 接入、业务台账、REST API 与 Webhook，参考 WVP-PRO。

## 技术栈

- 语言与运行时：Java 17（`release 17` 编译）
- 构建工具：Maven 3.9+
- 基础框架：Spring Boot 3.3.5
- Web 层：Spring MVC
- 持久层：MyBatis-Plus 3.5.9
- 数据库：PostgreSQL 14
- 缓存与会话：Redis / Valkey
- 参数校验：Bean Validation
- 接口文档：springdoc-openapi 2.6.0
- 测试：JUnit 5、Mockito、MockMvc
- 其他：Lombok、Spring Boot Configuration Processor

## 项目分层结构

单模块工程，包结构按职责分层：

- `controller`：HTTP API 入口，只做参数接收、校验、编排和结果返回
- `service`：业务编排、事务边界、查询组合、会话与缓存逻辑
- `mapper`：MyBatis-Plus 数据访问入口
- `entity`：数据库实体（继承 `persistence/BaseEntity`）
- `dto`：接口入参/出参（request / query / vo）、统一响应
- `common`：统一响应、错误码与异常、Web 基础设施、幂等、工具类
- `config`：Spring 配置、属性绑定、线程池、RestTemplate
- `security`：会话鉴权过滤器、角色校验、口令哈希、认证上下文
- `persistence`：实体基类、审计字段自动填充
- `integration`：外部系统调用封装

调用链约定：`controller -> service -> mapper/entity`。
禁止 `controller` 直接访问 `mapper`，禁止 `mapper` 反向依赖 Web 或安全层。

## 编码规范

- 使用 Java 17 语法，UTF-8 编码；注释与文档使用中文。
- Controller 保持薄层，不写复杂业务逻辑，不直接拼装数据库查询。
- 所有业务接口统一返回 `BaseResponse<T>`；分页统一返回 `PageResult<T>`（`items`、`page`、`page_size`、`total` 与 `data` 同级）。
- 字段命名：对外 JSON 全局 `snake_case`（`spring.jackson.property-naming-strategy`）；query 参数使用 `@RequestParam("page_size")` 显式绑定。
- 公共接口不暴露实体，统一使用 VO；入参使用 request DTO + Bean Validation。
- 服务层处理业务规则、状态校验、事务和幂等；写操作显式评估 `@Transactional` 边界。
- MyBatis-Plus 优先使用 `LambdaQueryWrapper` 和类型安全写法，避免字符串列名。
- Redis key 必须通过 `common/util/RedisKey` 构造，统一 `mgw:` 前缀，并设置 TTL。
- 日志不输出口令、令牌、密钥、完整会话令牌等敏感信息；访问日志由 `AccessLogFilter` 统一记录。
- 统一异常走 `GlobalExceptionHandler`，不要在 controller 内返回裸 JSON 错误体。

## API 与业务约束

- 错误响应保持稳定 `code`（与 HTTP 状态对齐）和可读 `message`；扩展业务错误码从 10000 起。
- 登录接口 `POST /api/auth/login`，受保护接口使用 `Authorization: Bearer <sessionToken>`。
- 角色校验使用 `@RequireRole({RoleType.ROLE_ADMIN, RoleType.ROLE_OPERATOR})`。
- 重试敏感的写接口使用 `@Idempotent` + `Idempotency-Key`。
- Webhook 入站接口（后续新增）必须验签、幂等、原始报文落库并异步处理，不得阻塞回调线程。
- 不在接口中混用多套响应格式；`extra` 只承载非标准附加元数据。

## 单元测试门禁

提交代码前，至少满足与变更范围匹配的测试门禁：

- 修改 controller、鉴权、异常处理、Web 配置时，必须补充或更新 Web 层测试。
- 修改 service 时，必须补充服务层单测，覆盖正常路径、异常路径和边界条件。
- 修改 mapper、entity、SQL、迁移脚本时，必须补充数据访问层验证，至少覆盖核心查询和写入条件。
- 修改统一响应、DTO、枚举、会话模型时，必须补充序列化或转换相关测试。
- 修改公共方法时，不允许只测 happy path，必须覆盖失败分支。
- 新增 bug 修复必须先补回归测试，再修代码。

### 质量要求

- 测试应可重复、独立运行，不依赖人工状态和本地脏数据；默认不连接真实 PostgreSQL/Redis。
- 单测优先使用 mock 隔离外部依赖。
- 断言必须具体，禁止只断言“无异常”或“返回非空”。
- 对外接口测试要覆盖状态码、响应体结构、关键字段和值（注意 snake_case 契约）。

### 最低门禁

```bash
mvn -o test          # 全部通过
mvn -o package -DskipTests
```

## 禁止事项

- 禁止提交真实口令、Token、密钥、数据库凭据和生产地址。
- 禁止绕过鉴权、角色校验、参数校验和异常处理。
- 禁止在 controller 中写重业务逻辑。
- 禁止把 entity 直接作为公共接口返回值。
- 禁止无测试覆盖就修改公共接口契约。
- 禁止无必要的大范围重命名、格式化和包结构调整。
- 禁止在业务代码中硬编码 Redis key、SQL 表名或外部系统地址。

## 当前开发重点

在脚手架基础上增量开发视频数据网关业务能力：

- ZLMediaKit REST API 对接与钩子接收
- 业务台账表设计与接口
- 对外 REST API（统一响应、鉴权、幂等、分页）
- 事件/Webhook 的可靠投递与重试
