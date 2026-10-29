# 新增业务模块指南

本脚手架的业务模块以 `module/demo` 为模板。下面以一个虚构的 `ipcamera`（摄像机）模块为例，
给出从表到接口的完整步骤；示例代码只展示写法，不代表真实业务。

## 0. 包结构约定

```
com.gjsk.mediagateway.module.<module>
├── controller/      # HTTP 入口（薄层）
├── service/         # 业务规则与事务
├── mapper/          # MyBatis-Plus Mapper
├── entity/          # 数据库实体（继承 BaseEntity）
└── dto/
    ├── request/     # 入参（Bean Validation）
    ├── query/       # 查询条件（继承 PageQuery）
    └── vo/          # 出参
```

模块之间不要互相访问 mapper；跨模块协作走 service 或领域事件。

## 1. 写迁移脚本

在 `migrations/versions/` 新增 `V2__ip_camera.sql`：

```sql
CREATE TABLE t_ip_camera
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY
        CONSTRAINT pk_t_ip_camera_id PRIMARY KEY,
    camera_id   VARCHAR(64)  NOT NULL
        CONSTRAINT uk_t_ip_camera_camera_id UNIQUE,

    name        VARCHAR(128) NOT NULL,
    stream_url  VARCHAR(512) NOT NULL,
    status      SMALLINT     NOT NULL DEFAULT 0,

    create_time TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_time TIMESTAMPTZ  NOT NULL DEFAULT now(),
    create_by   VARCHAR(64),
    update_by   VARCHAR(64),
    delete_flag SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT ck_t_ip_camera_status CHECK (status IN (0, 1))
);

CREATE INDEX idx_t_ip_camera_create_time ON t_ip_camera (create_time);
CREATE INDEX idx_t_ip_camera_status ON t_ip_camera (status);

CREATE TRIGGER trig_t_ip_camera_update
    BEFORE UPDATE ON t_ip_camera
    FOR EACH ROW EXECUTE FUNCTION func_update_time();

COMMENT ON TABLE t_ip_camera IS '摄像机表（示例）';
COMMENT ON COLUMN t_ip_camera.id IS '物理主键';
COMMENT ON COLUMN t_ip_camera.camera_id IS '业务主键，UUIDv7 字符串';
COMMENT ON COLUMN t_ip_camera.name IS '摄像机名称';
COMMENT ON COLUMN t_ip_camera.stream_url IS '拉流地址';
COMMENT ON COLUMN t_ip_camera.status IS '状态：0-停用 1-启用';
```

规范细节见 [db-conventions.md](db-conventions.md)。

## 2. 实体

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ip_camera")
public class IpCamera extends BaseEntity {

    private String cameraId;
    private String name;
    private String streamUrl;
    private Integer status;
}
```

- 继承 `BaseEntity` 即获得 `id`、审计字段、逻辑删除。
- 不写 getter/setter（Lombok），不把实体当接口返回值。

## 3. Mapper

```java
@Mapper
public interface IpCameraMapper extends BaseMapper<IpCamera> {
}
```

复杂查询使用 `@Select` 或 XML，列名必须与实体属性对齐（`map-underscore-to-camel-case=true`）。

## 4. DTO

```java
@Schema(description = "创建摄像机请求")
public record IpCameraCreateRequest(
        @NotBlank(message = "名称不能为空") @Size(max = 128) String name,
        @NotBlank(message = "拉流地址不能为空") @Size(max = 512) String streamUrl,
        @NotNull @Min(0) @Max(1) Integer status) {
}
```

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class IpCameraPageQuery extends PageQuery {
    private String name;
    private Integer status;
}
```

```java
@Schema(description = "摄像机")
public record IpCameraVO(
        String cameraId, String name, String streamUrl, Integer status,
        Instant createTime, Instant updateTime) {
}
```

## 5. Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class IpCameraService {

    private final IpCameraMapper ipCameraMapper;

    @Transactional
    public IpCameraVO create(IpCameraCreateRequest request) {
        IpCamera entity = new IpCamera();
        entity.setCameraId(IdUtils.newId());
        entity.setName(request.name().trim());
        entity.setStreamUrl(request.streamUrl().trim());
        entity.setStatus(request.status());
        ipCameraMapper.insert(entity);
        return toVO(entity);
    }

    public PageResult<IpCameraVO> page(IpCameraPageQuery query) {
        query.normalize();
        LambdaQueryWrapper<IpCamera> wrapper = new LambdaQueryWrapper<IpCamera>()
                .like(query.getName() != null, IpCamera::getName, query.getName())
                .eq(query.getStatus() != null, IpCamera::getStatus, query.getStatus())
                .orderByDesc(IpCamera::getCreateTime)
                .orderByDesc(IpCamera::getId);
        Page<IpCamera> page = ipCameraMapper.selectPage(
                new Page<>(query.currentPage(), query.currentPageSize()), wrapper);
        return PageResult.of(page, this::toVO);
    }

    public IpCameraVO detail(String cameraId) {
        IpCamera entity = ipCameraMapper.selectOne(
                new LambdaQueryWrapper<IpCamera>().eq(IpCamera::getCameraId, cameraId));
        if (entity == null) {
            throw new NotFoundException("摄像机不存在: " + cameraId);
        }
        return toVO(entity);
    }

    // update / delete 同理：先 require，再 updateById / deleteById，方法上加 @Transactional
}
```

## 6. Controller

```java
@Tag(name = "摄像机")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cameras")
public class IpCameraController {

    private final IpCameraService ipCameraService;

    @Idempotent(ttlSeconds = 300)
    @RequireRole({RoleType.ROLE_ADMIN, RoleType.ROLE_OPERATOR})
    @PostMapping
    public BaseResponse<IpCameraVO> create(@Valid @RequestBody IpCameraCreateRequest request) {
        return BaseResponse.ok(ipCameraService.create(request));
    }

    @GetMapping("/page")
    public BaseResponse<PageResult<IpCameraVO>> page(
            @RequestParam(required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        IpCameraPageQuery query = new IpCameraPageQuery();
        if (page != null) query.setPage(page);
        if (pageSize != null) query.setPageSize(pageSize);
        query.setName(name);
        query.setStatus(status);
        return BaseResponse.ok(ipCameraService.page(query));
    }

    @GetMapping("/{camera_id}")
    public BaseResponse<IpCameraVO> detail(@PathVariable("camera_id") String cameraId) {
        return BaseResponse.ok(ipCameraService.detail(cameraId));
    }
}
```

## 7. 测试

至少补两类：

- `IpCameraServiceTest`：mock mapper，覆盖创建、分页、详情、更新、删除、不存在等分支。
- `IpCameraControllerTest`：standalone MockMvc + `MvcTestSupport`，断言 HTTP 状态、`code`、snake_case 字段与分页结构。

命令：`mvn -o test`。

## 8. 文档与权限

- 更新 `docs/` 下的接口说明（或 OpenAPI 注解）。
- 确认公开路径、角色、幂等策略是否合适。
- 涉及新表时更新 `migrations/README.md` 版本表。

## 9. 接入 ZLMediaKit 的典型形态

| 形态 | 做法 |
|---|---|
| 调用 ZLM REST API | 注入 `ExternalHttpClient`，`get/post` 并处理业务响应；超时与 502 已统一 |
| 接收 ZLM 钩子 | 新增公开 controller（如 `POST /api/hooks/zlmediakit`）：验签 → 事件 ID 幂等 → 原始报文落表 → 快速返回 200 → `@Async` 异步处理 |
| 业务台账 | 按 [db-conventions.md](db-conventions.md) 建表；关键状态变更写操作记录（可先同步写，后续异步化） |

### Webhook 入站建议骨架

```java
@PostMapping("/api/hooks/zlmediakit")
public BaseResponse<HookAck> onHook(
        @RequestHeader(value = "X-Signature", required = false) String signature,
        @RequestBody String rawPayload) {
    webhookInboundService.receive("zlmediakit", signature, rawPayload); // 验签+幂等+落表+异步
    return BaseResponse.ok(new HookAck(true));
}
```

要点：

1. **验签**：HMAC-SHA256 + 时间戳窗口，密钥来自外部配置。
2. **幂等**：事件 ID 唯一索引，重复事件直接返回成功。
3. **异步**：回调线程只做落库与投递，重业务放到线程池/消息队列。
4. **重试**：处理失败记录状态与下次重试时间，由 `@Scheduled` 扫描重试；多实例需分布式锁。
5. **可观测**：记录原始报文、处理状态、错误信息，便于回放与排障。

## 10. 常见错误

- Controller 直接注入 Mapper（违反分层）。
- 用实体作为接口返回值（字段泄漏、无法演进契约）。
- 新增表不写注释、不建索引、用物理外键。
- 写接口没有事务或幂等；列表接口 N+1。
- 忘记给新接口配置公开路径或角色，导致 401/403。
- 改完接口不补测试、不同步文档。
