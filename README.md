# media-gateway

视频数据网关（Media Data Gateway）**通用脚手架**。技术栈与 magpie 完全一致，
把网关类系统必用的通用能力先做好，**不包含视频、流媒体、台账等具体业务**。

- **目标系统**：视频数据网关（ZLMediaKit 接入、业务台账、REST API + Webhook，参考 WVP-PRO）
- **本仓库定位**：通用底座。业务模块按 [docs/adding-a-module.md](docs/adding-a-module.md) 增量开发。
- **参考实现**：magpie（同技术栈、同响应规范、同分层约定、同测试门禁）。

## 技术栈

| 组件 | 选型 |
|---|---|
| 框架 | Spring Boot 3.3.5 |
| JDK | 17（`release 17` 编译） |
| 数据库 | PostgreSQL 14 |
| ORM | MyBatis-Plus 3.5.9 |
| 缓存 / 会话 | Redis / Valkey |
| 接口文档 | springdoc-openapi 2.6.0 |
| 构建 | Maven 3.9+ |
| 测试 | JUnit 5 + Mockito + MockMvc |
| 容器 | Dockerfile + docker compose |

## 已包含的通用能力

- 统一响应 `BaseResponse`、分页 `PageResult`、错误码 `ErrorCode`、全局异常处理
- 会话登录（Redis 会话令牌，滑动过期）+ 角色鉴权（`@RequireRole`）
- 幂等控制（`@Idempotent` + `Idempotency-Key` 请求头）
- 链路追踪 `X-Request-Id` + 访问日志 + 异步线程 MDC 透传
- MyBatis-Plus + PostgreSQL：双主键、审计字段自动填充、逻辑删除、分页拦截器
- 外部 HTTP 调用封装（统一超时、traceId 透传、上游错误统一映射）
- OpenAPI/Swagger、健康检查、数据库迁移脚本、Dockerfile 与 compose
- 示例业务模块（`module/demo`，复制起点，不是业务功能）
- 85 个单元测试，零外部依赖（不连 PostgreSQL / Redis）

## 目录结构

```
media-gateway/
├── pom.xml  README.md  CODEX.md  Dockerfile  docker-compose.yml
├── .gitignore  .dockerignore  .env.example
├── .claude/rules/api-response-style.md
├── .codex/skills/java-springboot-backend/          # AI 编码规范（同 magpie）
├── docs/                                          # 架构 / 接口 / 数据库 / 新增模块
├── migrations/versions/V1__init_schema.sql        # 手工迁移脚本
└── src/
    ├── main/resources/{application.yml, logback-spring.xml}
    ├── main/java/com/gjsk/mediagateway/
    │   ├── MediaGatewayApplication.java
    │   ├── common/api/        # BaseResponse / PageResult / PageQuery
    │   ├── common/error/      # ErrorCode / BusinessException 及子类
    │   ├── common/web/        # 全局异常、traceId、访问日志、Web 装配、OpenAPI
    │   ├── common/idempotent/ # @Idempotent 与拦截器
    │   ├── common/util/       # JsonUtils / IdUtils(UUIDv7) / RedisKey
    │   ├── config/            # 配置属性、RestTemplate、线程池、MyBatis-Plus
    │   ├── security/          # 会话鉴权过滤器、角色校验、口令哈希、上下文
    │   ├── service/           # SessionService / AuthService
    │   ├── persistence/       # BaseEntity / 审计字段自动填充
    │   ├── integration/       # ExternalHttpClient（上游调用）
    │   ├── controller/        # AuthController / HealthController
    │   └── module/demo/       # 示例业务模块（模板）
    └── test/java/...          # 85 个测试
```

## 快速开始

### 1. 环境要求

- JDK 17+（本机 JDK 21 也可，`release 17` 编译出 17 字节码）
- Maven 3.9+
- PostgreSQL 14、Redis 6+（可用 docker compose 启动）

### 2. 启动本地依赖并初始化数据库

```bash
cp .env.example .env                     # 按需修改数据库口令
docker compose up -d postgres redis
docker compose exec -T postgres psql -U postgres -d media_gateway_db \
  < migrations/versions/V1__init_schema.sql
```

没有 Docker 时，手动准备 PostgreSQL 与 Redis，并执行 `migrations/versions/V1__init_schema.sql`。

### 3. 编译与测试

```bash
mvn -o test                # 85 个测试，全部不依赖外部服务
mvn -o package -DskipTests # 产出 target/media-gateway-0.1.0.jar
```

### 4. 启动

```bash
mvn spring-boot:run
# 或
java -jar target/media-gateway-0.1.0.jar
```

默认：端口 `10040`、上下文路径 `/mgw`、库 `media_gateway_db`、Redis db `3`。

### 5. 登录获取会话令牌

```bash
curl -s -X POST http://localhost:10040/mgw/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

返回示例：

```json
{
  "code": 0,
  "message": "ok",
  "data": {"token": "…32位十六进制…", "token_type": "Bearer", "expires_in": 7200},
  "extra": null
}
```

后续请求统一携带 `Authorization: Bearer <token>`。

### 6. 调用示例接口

```bash
TOKEN=<上一步返回的 token>

# 分页查询（登录即可访问）
curl -s "http://localhost:10040/mgw/api/demo/items/page?page=1&page_size=20" \
  -H "Authorization: Bearer $TOKEN"

# 新增（需要 ADMIN/OPERATOR 角色 + Idempotency-Key）
curl -s -X POST http://localhost:10040/mgw/api/demo/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-create-0001" \
  -d '{"name":"示例记录","status":1,"remark":"hello"}'
```

### 7. 接口文档与健康检查

| 地址 | 用途 |
|---|---|
| `http://localhost:10040/mgw/swagger-ui.html` | Swagger UI |
| `http://localhost:10040/mgw/v3/api-docs` | OpenAPI JSON |
| `http://localhost:10040/mgw/health` | 健康检查（公开） |

## 认证与角色

- 登录成功后生成 128 位随机会话令牌，会话信息存 Redis：`mgw:session:{token}`，默认 TTL 7200 秒，访问自动续期。
- 鉴权由 `SessionAuthFilter` 完成；公开路径在 `mgw.security.public-paths` 配置（Ant 风格）。
- 角色：`ADMIN`（管理员）、`OPERATOR`（操作员）、`VIEWER`（只读），通过 `@RequireRole` 标注在方法或类上。
- 开发账号（仅本地默认值）：

| 账号 | 口令 | 角色 |
|---|---|---|
| `admin` | `admin123` | ADMIN, OPERATOR |
| `viewer` | `viewer123` | VIEWER |

生产环境必须通过外部配置或环境变量覆盖默认账号口令，或替换 `AuthService` 接入公司统一身份系统。
默认口令以 PBKDF2-HMAC-SHA256 哈希存储，生成新哈希：

```bash
jshell --class-path target/classes
jshell> import com.gjsk.mediagateway.security.PasswordHasher;
jshell> PasswordHasher.hash("新口令");
```

## 配置

配置优先级（高 → 低）：命令行参数 > 环境变量 > jar 同级 `config/application.yml` > jar 内 `application.yml`。
外部配置文件只需写需要覆盖的项，且已被 `.gitignore` 忽略，禁止提交真实凭据。

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `server.port` | `10040` | 服务端口（`MGW_PORT`） |
| `server.servlet.context-path` | `/mgw` | 上下文路径（`MGW_CONTEXT_PATH`） |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/media_gateway_db` | `MGW_DB_URL` |
| `spring.datasource.username` | `postgres` | `MGW_DB_USERNAME` |
| `spring.datasource.password` | `postgres` | `MGW_DB_PASSWORD` |
| `spring.data.redis.host` | `localhost` | `MGW_REDIS_HOST` |
| `spring.data.redis.port` | `6379` | `MGW_REDIS_PORT` |
| `spring.data.redis.database` | `3` | `MGW_REDIS_DB` |
| `mgw.security.public-paths` | 见 application.yml | 免登录路径 |
| `mgw.security.session.ttl-seconds` | `7200` | 会话有效期（`MGW_SESSION_TTL_SECONDS`） |
| `mgw.security.users` | admin/viewer | 登录账号，生产用外部配置覆盖 |
| `mgw.http.connect-timeout-millis` | `3000` | 上游连接超时 |
| `mgw.http.read-timeout-millis` | `10000` | 上游读取超时 |
| `mgw.app.version` | `dev` | 健康检查与文档展示版本 |
| `springdoc.api-docs.enabled` | `true` | 文档开关（`MGW_API_DOCS_ENABLED`） |

Redis 密码若需要，使用标准 Spring 环境变量 `SPRING_DATA_REDIS_PASSWORD` 注入。

## 数据库

- 迁移脚本：`migrations/versions/`，按版本号手工执行，规则见 [migrations/README.md](migrations/README.md)。
- 设计规范：双主键、审计字段、逻辑删除、禁物理外键、`TIMESTAMPTZ`、全量注释，见 [docs/db-conventions.md](docs/db-conventions.md)。
- 实体继承 `persistence/BaseEntity` 即自动获得物理主键 + 审计字段 + 逻辑删除，审计字段由 `AuditMetaObjectHandler` 自动填充。

## 幂等

写接口标注 `@Idempotent(ttlSeconds = 300)` 后，调用方必须携带 `Idempotency-Key`（8-128 位字母/数字/`_ . : -`）；
同一用户 + 方法 + 路径 + key 在 TTL 内重复提交返回 `409`。实现基于 Redis `SETNX`。

## 外部系统调用

注入 `integration/ExternalHttpClient` 调用上游（例如后续接入 ZLMediaKit REST API）：

```java
Map<String, Object> result = externalHttpClient.get("zlmediakit", url, Map.class);
```

统一处理超时/网络异常（映射为 502），并自动透传 `X-Request-Id`。

## 测试

- 全部测试不依赖外部 PostgreSQL / Redis，`mvn -o test` 可直接执行。
- 覆盖：统一响应/分页/错误码、全局异常、traceId、会话鉴权过滤器、会话服务、认证服务、角色拦截器、幂等拦截器、示例模块 Service/Controller、UUIDv7、PBKDF2、Spring 上下文装配。
- 提交前门禁见 [CODEX.md](CODEX.md) 与 [docs/adding-a-module.md](docs/adding-a-module.md)。

## 容器化

```bash
mvn -o clean package -DskipTests
docker compose up -d --build
```

`docker-compose.yml` 包含 PostgreSQL、Redis、应用三个服务；数据库迁移需按上文手工执行一次。

## 如何新增业务模块

完整步骤与代码模板见 [docs/adding-a-module.md](docs/adding-a-module.md)，概要：

1. 写迁移脚本（新表 + 索引 + 注释）
2. 实体继承 `BaseEntity`，Mapper 继承 `BaseMapper`
3. 建 DTO（request / query / vo），使用 Bean Validation
4. Service 承载业务规则与事务边界（`@Transactional`）
5. Controller 薄层 + 统一 `BaseResponse` + 必要时 `@RequireRole` / `@Idempotent`
6. 补 Service/Controller 测试，更新文档

## 对接目标场景的建议

| 目标能力 | 本脚手架对应位置 | 说明 |
|---|---|---|
| REST API（开放给业务/管理端） | `controller` + `service` + 统一响应 | 按 `module/demo` 复制 |
| 调用 ZLMediaKit REST API | `integration/ExternalHttpClient` | 统一超时、错误映射、traceId 透传 |
| 接收 ZLMediaKit 钩子（Webhook 入站） | 新增 `module/webhook` 或 `module/zlmediakit` | 建议：独立公开路径 + 验签 + 幂等 + 原始报文落表 + 异步处理，避免阻塞回调 |
| 业务台账 | 业务模块表 + `migrations` | 按 `docs/db-conventions.md` 设计；审计/操作记录可参考 `BaseEntity` |
| 出站 Webhook（可选） | 新增 `module/webhook/outbound` | `@Scheduled` + 退避重试 + 死信表；多实例部署需分布式锁 |

> 提示：Webhook 入站接口必须做验签（HMAC）、幂等（事件 ID 唯一索引）与异步化，不要直接在执行线程里做重业务。

## 已知限制 / 后续可扩展

- 登录账号来自配置，尚未接入数据库用户表或 Keycloak/MaxKey 等统一身份系统（替换 `AuthService` 即可）。
- 未预置 Webhook 收发管道、操作日志表（按 A 档范围裁剪，接入方式见上文与 `docs/adding-a-module.md`）。
- 未引入 Flyway/Liquibase，迁移脚本按版本手工执行（与 magpie 保持一致）。
- `@Scheduled` / `@Async` 已装配，但默认单实例运行；多实例定时任务需分布式锁。
- 本机 Docker 未启动时无法执行 compose 冒烟，需先启动 Docker Desktop 或准备本地 PostgreSQL/Redis。
