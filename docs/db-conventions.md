# 数据库设计规范

本项目使用 PostgreSQL 14，规范沿用 magpie 与《PostgreSQL 数据库设计规范》。
完整参考见 `.codex/skills/java-springboot-backend/references/pq-schema-style.md`。

## 1. 建库与 schema

```sql
CREATE DATABASE media_gateway_db
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'C'
         LC_CTYPE = 'C'
         TEMPLATE = template0
         CONNECTION LIMIT = -1;

ALTER DATABASE media_gateway_db SET timezone TO 'Asia/Shanghai';
ALTER DATABASE media_gateway_db SET default_transaction_isolation TO 'read committed';
```

- 生产建议独立业务 schema（如 `app_schema`）；脚手架默认使用 `public`，与 magpie 保持一致。
- 时间统一 `TIMESTAMPTZ`，库级默认东八区。

## 2. 双主键

| 主键 | 类型 | 用途 |
|---|---|---|
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY` | 物理主键，仅数据库内部索引/关联，禁止业务使用 |
| `xxx_id` | `VARCHAR(64)` | 业务主键，UUIDv7 十六进制（32 位，去连字符），对外接口使用 |

- Java 侧通过 `common/util/IdUtils.newId()` 生成业务主键。
- `BaseEntity.id` 使用 MyBatis-Plus `IdType.AUTO`。

## 3. 审计字段

所有业务表末尾固定包含、固定顺序：

```sql
create_time TIMESTAMPTZ NOT NULL DEFAULT now(),
update_time TIMESTAMPTZ NOT NULL DEFAULT now(),
create_by   VARCHAR(64),
update_by   VARCHAR(64),
delete_flag SMALLINT    NOT NULL DEFAULT 0,
```

- `create_time/update_time/create_by/update_by` 由 `AuditMetaObjectHandler` 自动填充。
- `update_time` 同时由触发器兜底（应用层与触发器的值都以当前时间为准）。
- 追加型日志表（操作日志等）可只保留 `create_time`，需在表注释中说明例外原因。

## 4. 逻辑删除

- 统一 `delete_flag SMALLINT`：`0`-未删除、`1`-已删除。
- 实体字段标注 `@TableLogic`；MyBatis-Plus 查询自动追加 `delete_flag = 0`。
- 禁止对业务数据执行物理 `DELETE`；确需清理数据走运维脚本并记录。

## 5. 外键、索引、约束

- **禁止物理外键**，使用逻辑外键 + 索引。
- 必须建索引的字段：逻辑外键、高频过滤字段、时间字段、唯一业务号。
- 命名：`idx_表名_字段`、`uk_表名_字段`、`pk_表名_字段`、`ck_表名_字段`。
- 单表索引数量 ≤ 10，避免冗余索引。
- 状态字段用 `SMALLINT` + `CHECK` 约束。

## 6. 类型规范

| 场景 | 类型 |
|---|---|
| 业务主键 | `VARCHAR(64)`（UUIDv7 hex） |
| 物理主键 | `BIGINT GENERATED ALWAYS AS IDENTITY` |
| 时间 | `TIMESTAMPTZ` |
| 状态 | `SMALLINT` |
| 金额 | `DECIMAL(18,2)` |
| 名称/备注 | `VARCHAR(n)`，按业务上限设置 |
| 灵活元数据 | `JSONB`（不作为高频过滤条件） |

## 7. 注释

- 每张表、每个字段、每个约束都必须有 `COMMENT ON ...`。
- 表注释写清用途与生命周期；状态字段注释列出全部取值。

## 8. 迁移流程

- 目录：`migrations/versions/`，命名 `V{序号}__{说明}.sql`（与 magpie 一致，未引入 Flyway）。
- 只增不改：已执行脚本禁止修改，变更走新版本脚本。
- 每个脚本按顺序执行一次，执行后记录到 `migrations/README.md` 的版本表。
- 脚本必须包含：建表、索引、触发器（如需要）、全量注释。

```bash
docker compose exec -T postgres psql -U postgres -d media_gateway_db \
  < migrations/versions/V1__init_schema.sql
```

## 9. 示例（V1 示例表）

```sql
CREATE TABLE t_demo_item
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY
        CONSTRAINT pk_t_demo_item_id PRIMARY KEY,
    item_id     VARCHAR(64)  NOT NULL
        CONSTRAINT uk_t_demo_item_item_id UNIQUE,

    name        VARCHAR(128) NOT NULL,
    status      SMALLINT     NOT NULL DEFAULT 0,
    remark      VARCHAR(512),

    create_time TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_time TIMESTAMPTZ  NOT NULL DEFAULT now(),
    create_by   VARCHAR(64),
    update_by   VARCHAR(64),
    delete_flag SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT ck_t_demo_item_status CHECK (status IN (0, 1))
);

CREATE INDEX idx_t_demo_item_create_time ON t_demo_item (create_time);
CREATE INDEX idx_t_demo_item_status ON t_demo_item (status);
```

对应实体继承 `BaseEntity`，仅补充业务字段：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_demo_item")
public class DemoItem extends BaseEntity {
    private String itemId;
    private String name;
    private Integer status;
    private String remark;
}
```

## 10. 查询与事务约定

- 查询优先 `LambdaQueryWrapper`，禁止字符串列名。
- 分页统一 `Page<T>` + `PageResult.of(...)`；列表接口避免 N+1。
- 写操作显式评估 `@Transactional`；事务内不做慢网络调用。
- 更新必须带明确 `where` 条件，禁止无条件下全表更新。
- 大字段/日志类数据不要与核心业务表混存。
