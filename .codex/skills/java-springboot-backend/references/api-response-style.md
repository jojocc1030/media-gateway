# API 响应风格

## 基础结构

所有业务 API 统一返回 `BaseResponse`（`com.gjsk.mediagateway.common.api.BaseResponse`）：

```json
{
  "code": 0,
  "message": "ok",
  "data": { ... },
  "extra": null
}
```

- `code`：`0` 表示成功；错误码与 HTTP 状态码对齐（400/401/403/404/405/409/429/500/502），业务扩展码从 10000 起。
- `message`：成功固定 `ok`；失败为可读错误信息。
- `data`：业务数据；错误或无数据时为 `null`。
- `extra`：仅承载非标准附加元数据，常规业务数据一律放 `data`，禁止把分页字段放到 `extra`。

## 分页响应

分页字段（`page` / `page_size` / `total`）与 `items` 同级放在 `data` 内：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "items": [ ... ],
    "page": 1,
    "page_size": 20,
    "total": 128
  }
}
```

- 页码从 1 开始，默认 `page=1`、`page_size=20`，`page_size` 上限 100，越界由 `PageQuery.normalize()` 收敛。
- 使用 `PageResult.of(...)` 构造，禁止手写分页字段名。

## 字段命名

- 对外 JSON 全局 `snake_case`（Spring Jackson 全局命名策略），Java 字段保持 camelCase。
- query 参数必须显式使用 snake_case 名称：`@RequestParam(value = "page_size")`。
- 时间统一 ISO-8601（如 `2026-01-01T00:00:00Z`）。

## 错误响应

```json
{
  "code": 401,
  "message": "会话已失效，请重新登录",
  "data": null,
  "extra": null
}
```

- HTTP 状态码与 `code` 保持一致（由 `GlobalExceptionHandler` 保证）。
- 4xx 不返回堆栈；5xx 对外统一“服务器内部错误”，细节只写日志。

## 实现约定

```java
// 单条数据
return BaseResponse.ok(vo);

// 分页
return BaseResponse.ok(PageResult.of(page, this::toVO));

// 无数据
return BaseResponse.ok();

// 错误：抛业务异常，由全局异常处理转换
throw new NotFoundException("资源不存在: " + id);
```

- 禁止 controller 直接构造裸错误 JSON。
- 禁止在接口中混用多种响应结构。
