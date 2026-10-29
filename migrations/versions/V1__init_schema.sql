-- ============================================================================
-- media-gateway 初始化脚本 V1
-- 执行方式：psql -h <host> -U <user> -d media_gateway_db -f V1__init_schema.sql
-- 规范：BIGINT 物理主键 + VARCHAR(64) 业务主键（UUIDv7）、禁物理外键、
--       TIMESTAMPTZ 时间、审计字段固定末尾、表/字段/约束全量注释。
-- ============================================================================

SET search_path TO public;

-- 1. 通用更新时间触发器函数 -----------------------------------------------------
CREATE OR REPLACE FUNCTION func_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. 示例业务表 ----------------------------------------------------------------
CREATE TABLE t_demo_item
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY
        CONSTRAINT pk_t_demo_item_id PRIMARY KEY,
    item_id     VARCHAR(64)  NOT NULL
        CONSTRAINT uk_t_demo_item_item_id UNIQUE,

    -- 业务字段
    name        VARCHAR(128) NOT NULL,
    status      SMALLINT     NOT NULL DEFAULT 0,
    remark      VARCHAR(512),

    -- 审计字段（固定顺序、固定位于表末尾）
    create_time TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_time TIMESTAMPTZ  NOT NULL DEFAULT now(),
    create_by   VARCHAR(64),
    update_by   VARCHAR(64),
    delete_flag SMALLINT     NOT NULL DEFAULT 0,

    -- 业务约束
    CONSTRAINT ck_t_demo_item_status CHECK (status IN (0, 1))
);

CREATE INDEX idx_t_demo_item_create_time ON t_demo_item (create_time);
CREATE INDEX idx_t_demo_item_status ON t_demo_item (status);

CREATE TRIGGER trig_t_demo_item_update
    BEFORE UPDATE
    ON t_demo_item
    FOR EACH ROW
EXECUTE FUNCTION func_update_time();

COMMENT ON TABLE t_demo_item IS '示例业务表，仅用于演示实体/迁移/接口规范，可整表删除';
COMMENT ON COLUMN t_demo_item.id IS '物理主键，BIGINT 自增，无业务含义，仅用于数据库内部索引和关联';
COMMENT ON COLUMN t_demo_item.item_id IS '业务主键，UUIDv7 字符串，全局唯一，对外接口使用';
COMMENT ON COLUMN t_demo_item.name IS '名称';
COMMENT ON COLUMN t_demo_item.status IS '状态：0-停用 1-启用';
COMMENT ON COLUMN t_demo_item.remark IS '备注';
COMMENT ON COLUMN t_demo_item.create_time IS '审计字段，创建时间，默认当前北京时间';
COMMENT ON COLUMN t_demo_item.update_time IS '审计字段，更新时间，由触发器维护';
COMMENT ON COLUMN t_demo_item.create_by IS '审计字段，创建人用户 ID';
COMMENT ON COLUMN t_demo_item.update_by IS '审计字段，最后更新人用户 ID';
COMMENT ON COLUMN t_demo_item.delete_flag IS '审计字段，逻辑删除标记：0-未删除 1-已删除';
COMMENT ON CONSTRAINT pk_t_demo_item_id ON t_demo_item IS '示例表物理主键约束';
COMMENT ON CONSTRAINT uk_t_demo_item_item_id ON t_demo_item IS '示例表业务主键唯一约束';
COMMENT ON CONSTRAINT ck_t_demo_item_status ON t_demo_item IS '示例表状态检查约束，仅允许 0/1';
