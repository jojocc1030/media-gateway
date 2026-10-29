# 接口规范

## 1. 基础信息

| 项 | 值 |
|---|---|
| 服务地址 | `http://<host>:10040` |
| 上下文路径 | `/mgw`（`MGW_CONTEXT_PATH`） |
| 业务接口前缀 | `/mgw/api` |
| 健康检查 | `GET /mgw/health`（公开） |
| 接口文档 | `GET /mgw/swagger-ui.html`、`GET /mgw/v3/api-docs` |
| 编码 | UTF-8 |
| 时间格式 | ISO-8601 带时区，如 `2026-01-01T00:00:00Z` |
| 字段命名 | 全局 snake_case |

## 2. 统一响应

```json
{
  "code": 0,
  "message": "ok",
  "data": { },
  "extra": null
}
```

- `code=0` 成功；失败时 `code` 与 HTTP 状态一致。
- `extra` 仅用于非标准元数据；分页字段不允许放 `extra`。

### 错误码

| code | HTTP | 含义 |
|---|---|---|
| 0 | 200 | 成功 |
| 400 | 400 | 请求参数错误 / 校验失败 |
| 401 | 401 | 未认证、令牌缺失或会话失效 |
| 403 | 403 | 角色权限不足 |
| 404 | 404 | 资源不存在 |
| 405 | 405 | 请求方法不允许 |
| 409 | 409 | 资源冲突 / 重复提交 |
| 429 | 429 | 请求过于频繁（预留） |
| 500 | 500 | 服务器内部错误 |
| 502 | 502 | 上游服务异常 |
| 10000+ | 自定义 | 业务扩展错误码（建议随模块分配段） |

## 3. 分页

请求：

```
GET /mgw/api/demo/items/page?page=1&page_size=20&name=关键字&status=1
```

响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "items": [ ],
    "page": 1,
    "page_size": 20,
    "total": 128
  },
  "extra": null
}
```

- `page` 从 1 开始；`page_size` 默认 20、上限 100；越界自动收敛。
- 排序默认按创建时间倒序，`id` 兜底保证稳定。

## 4. 认证

### 4.1 登录

```http
POST /mgw/api/auth/login
Content-Type: application/json

{"username":"admin","password":"admin123"}
```

```json
{
  "code": 0,
  "message": "ok",
  "data": {"token":"…","token_type":"Bearer","expires_in":7200},
  "extra": null
}
```

### 4.2 携带令牌

```http
Authorization: Bearer <token>
```

### 4.3 登出 / 当前用户

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/mgw/api/auth/logout` | 删除当前会话 |
| GET | `/mgw/api/auth/me` | 返回 `user_id`、`username`、`roles` |

### 4.4 公开路径

见 `mgw.security.public-paths`，默认：`/health`、`/api/auth/login`、`/api/public/**`、`/v3/api-docs/**`、`/swagger-ui/**`、`/swagger-ui.html`。

> 后续新增 Webhook 入站接口时，应单独配置公开路径并自行做签名校验，不能复用会话鉴权。

## 5. 角色

| 角色 | 说明 |
|---|---|
| `ADMIN` | 管理员，读写全量 |
| `OPERATOR` | 操作员，业务读写 |
| `VIEWER` | 只读 |

- 接口通过 `@RequireRole({RoleType.ROLE_ADMIN, RoleType.ROLE_OPERATOR})` 声明，方法级优先于类级。
- 无权限返回 `403`，`message` 提示所需角色。
- 未标注注解的接口只要求登录。

## 6. 幂等

重试敏感的写接口使用 `@Idempotent`：

```http
POST /mgw/api/demo/items
Authorization: Bearer <token>
Idempotency-Key: demo-create-0001
```

- `Idempotency-Key`：8-128 位字母/数字/`_ . : -`。
- 同一用户 + 方法 + 路径 + key 在 TTL 内重复提交返回 `409`。
- 缺失或非法 key 返回 `400`。
- key 建议使用业务唯一号（如订单号、事件 ID），不要用随机值。

## 7. 链路追踪

- 请求可携带 `X-Request-Id`（8-64 位字母/数字/`-`/`_`）；未携带则服务端生成。
- 响应头回写 `X-Request-Id`；调用上游时自动透传。
- 排障时按 `X-Request-Id` 搜索日志。

## 8. 示例

```bash
BASE=http://localhost:10040/mgw

TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 分页
curl -s "$BASE/api/demo/items/page?page=1&page_size=20" -H "Authorization: Bearer $TOKEN"

# 新增（幂等）
curl -s -X POST "$BASE/api/demo/items" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-create-0001' \
  -d '{"name":"示例记录","status":1,"remark":"hello"}'

# 当前用户
curl -s "$BASE/api/auth/me" -H "Authorization: Bearer $TOKEN"
```

## 9. 变更管理

- 新增/修改接口必须同步：OpenAPI 注解（`@Operation`/`@Schema`）、本文档、Controller 测试。
- 不兼容变更（字段重命名、删除、语义变化）需要版本化路径或兼容期，禁止静默修改契约。
- 公共字段命名保持 snake_case；Java 内部保持 camelCase，由全局 Jackson 策略转换。
