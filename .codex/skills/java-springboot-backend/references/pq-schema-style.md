# PostgreSQL数据库设计规范（精简）

版本：V1\.0
适用：PostgreSQL 14/17、云厂商 RDS PG
场景：单体、微服务、分布式、分库分表
时区默认：Asia/Shanghai 北京时间

## 前置：生产环境建库&Schema

### 1\. 建库语句（统一 UTF8 \+ 北京时间时区）

```sql
-- 创建业务数据库：UTF8编码、C排序规则、独立模板、默认北京时间时区
CREATE DATABASE business_db
    WITH 
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0
    CONNECTION LIMIT = -1;

-- 全局数据库默认时区设为北京时间
ALTER DATABASE business_db SET timezone TO 'Asia/Shanghai';
-- 事务隔离级别默认读已提交
ALTER DATABASE business_db SET default_transaction_isolation TO 'read committed';
-- 默认搜索路径配置
ALTER DATABASE business_db SET search_path TO public,app_schema;
```

### 2\. 建 Schema 语句（业务隔离、权限可控）

```sql
-- 切换至目标数据库
\c business_db;

-- 创建业务专属Schema，不使用public存放业务表
CREATE SCHEMA IF NOT EXISTS app_schema;
COMMENT ON SCHEMA app_schema IS '业务核心Schema，存储所有业务表、索引、视图';

-- 会话默认Schema
SET search_path TO app_schema;
-- 账号永久默认Schema（替换为实际数据库登录账号）
ALTER ROLE db_user SET search_path TO app_schema;
```

## 一、核心设计原则（建议遵循）

1. **双主键设计**：物理主键（BIGINT 自增，无业务含义，禁止业务流程使用）\+ 业务主键（UUID v7 原生类型，全局唯一对外交互）；

2. **表名规范**：统一小写 \+ 下划线，标准前缀 `t_模块_表名`；表名过长可简写为 `模块_表名`；

3. **外键规范**：**完全禁用物理外键**，采用逻辑外键 \+ 索引替代，解耦表结构、适配分库分表；

4. **索引规范**：逻辑外键字段、高频查询字段、时间字段必建索引；单表索引数量≤10 个，杜绝冗余索引；

5. **通用审计字段**：所有业务表统一包含 4～5 个审计字段，**固定顺序、固定放在表末尾**：
`create_time / update_time / create_by / update_by / delete_flag`，跨项目统一风格，不随意增减或变更顺序；

6. **字段类型规范**：时间统一用 `TIMESTAMPTZ`（带时区，适配北京时间）、状态数量较少时用 `SMALLINT`，较多时可酌情使用VARCHAR类型（语义理解更强、维护更简便）、金额用 `DECIMAL\(18,2\)`、UUID 建议采用原生 `UUID` 类型，避免用 VARCHAR 存储；

7. **时区规范**：库级、会话级统一默认 **Asia/Shanghai 北京时间**，所有时间字段基于东八区存储展示。

## 二、关键命名规范/推荐实践

- 表名：`t_模块_表名`
示例：`t_user_info`、`t_order_info`、`t_mts_dataset`、`t_mts_model`

- 索引命名：
单字段：`idx_表名_字段名`
联合字段：`idx_表名_字段1_字段2`

- 约束命名：
主键：`pk_表名_字段`
唯一约束：`uk_表名_字段`
检查约束：`ck_表名_字段`（视情况可仅在应用层面做约束）

## 三、DDL 模板

### 3\.1 模板 1：用户表（基准模板）

```sql
CREATE TABLE t_user_info (
    -- 双主键
    id          BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_t_user_info_id PRIMARY KEY,
    user_id     UUID NOT NULL CONSTRAINT uk_t_user_info_user_id UNIQUE,
    
    -- 业务字段
    username    VARCHAR(64) NOT NULL,
    phone       VARCHAR(32),
    email       VARCHAR(128),
    password    VARCHAR(128) NOT NULL,
    status      SMALLINT NOT NULL DEFAULT 0,
    avatar      VARCHAR(255),
    
    -- 逻辑外键
    dept_id     UUID,
    role_id     UUID,
    
    -- 审计字段（固定末尾固定顺序）
    create_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    create_by   UUID,
    update_by   UUID,
    delete_flag SMALLINT NOT NULL DEFAULT 0,
    
    -- 业务检查约束
    CONSTRAINT ck_t_user_info_status CHECK (status IN (0, 1, 2))
);

-- 业务索引
CREATE INDEX idx_t_user_info_dept_id ON t_user_info(dept_id);
CREATE INDEX idx_t_user_info_role_id ON t_user_info(role_id);
CREATE INDEX idx_t_user_info_phone ON t_user_info(phone);
CREATE INDEX idx_t_user_info_create_time ON t_user_info(create_time);

-- 表与字段完整注释
COMMENT ON TABLE t_user_info IS '用户信息表，存储系统所有用户的基础信息、权限关联信息';
COMMENT ON COLUMN t_user_info.id IS '物理主键，BIGINT自增，无业务含义，仅用于数据库内部索引、关联';
COMMENT ON COLUMN t_user_info.user_id IS '业务主键，UUID v7原生类型，全局唯一，用于业务交互、API接口标识';
COMMENT ON COLUMN t_user_info.username IS '用户名，系统登录账号，不可为空，用于用户身份识别';
COMMENT ON COLUMN t_user_info.phone IS '用户手机号，可选，用于登录验证、短信通知';
COMMENT ON COLUMN t_user_info.email IS '用户邮箱，可选，用于登录验证、邮件通知';
COMMENT ON COLUMN t_user_info.password IS '用户密码，加密存储，不可为空，用于登录校验';
COMMENT ON COLUMN t_user_info.status IS '用户状态：0-正常 1-禁用 2-锁定，控制用户登录权限';
COMMENT ON COLUMN t_user_info.avatar IS '用户头像地址，可选，用于页面展示';
COMMENT ON COLUMN t_user_info.dept_id IS '逻辑外键，关联部门表t_dept_info的dept_id，标识用户所属部门';
COMMENT ON COLUMN t_user_info.role_id IS '逻辑外键，关联角色表t_role_info的role_id，标识用户所属角色';
COMMENT ON COLUMN t_user_info.create_time IS '审计字段，数据创建时间，默认当前北京时间';
COMMENT ON COLUMN t_user_info.update_time IS '审计字段，数据更新时间，默认当前北京时间，应用层同步更新';
COMMENT ON COLUMN t_user_info.create_by IS '审计字段，数据创建人，关联用户表t_user_info的user_id';
COMMENT ON COLUMN t_user_info.update_by IS '审计字段，数据更新人，关联用户表t_user_info的user_id';
COMMENT ON COLUMN t_user_info.delete_flag IS '审计字段，逻辑删除标记：0-未删除 1-已删除，默认未删除';
COMMENT ON CONSTRAINT pk_t_user_info_id ON t_user_info IS '用户表物理主键约束，唯一标识表中每条记录';
COMMENT ON CONSTRAINT uk_t_user_info_user_id ON t_user_info IS '用户表业务主键唯一约束，保证user_id全局唯一';
COMMENT ON CONSTRAINT ck_t_user_info_status ON t_user_info IS '用户状态检查约束，限制状态值仅可取值0、1、2';
```

### 3\.2 模板 2：订单表

```sql
CREATE TABLE t_order_info (
    id              BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_t_order_info_id PRIMARY KEY,
    order_id        UUID NOT NULL CONSTRAINT uk_t_order_info_order_id UNIQUE,
    
    -- 业务字段
    user_id         UUID NOT NULL,
    product_id      UUID NOT NULL,
    order_no        VARCHAR(64) NOT NULL UNIQUE,
    amount          DECIMAL(18,2) NOT NULL,
    order_status    SMALLINT NOT NULL DEFAULT 0,
    pay_type        SMALLINT,
    pay_time        TIMESTAMPTZ,
    
    -- 审计字段
    create_time     TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_time     TIMESTAMPTZ NOT NULL DEFAULT now(),
    create_by       UUID,
    update_by       UUID,
    delete_flag     SMALLINT NOT NULL DEFAULT 0,
    
    -- 业务约束
    CONSTRAINT ck_t_order_info_status CHECK (order_status IN (0,1,2,3,4,5))
);

-- 业务索引
CREATE INDEX idx_t_order_info_user_id ON t_order_info(user_id);
CREATE INDEX idx_t_order_info_product_id ON t_order_info(product_id);
CREATE INDEX idx_t_order_info_order_status ON t_order_info(order_status);
CREATE INDEX idx_t_order_info_create_time ON t_order_info(create_time);

-- 完整注释
COMMENT ON TABLE t_order_info IS '订单信息表，存储系统所有订单的基础信息、支付状态、关联信息';
COMMENT ON COLUMN t_order_info.id IS '物理主键，BIGINT自增，无业务含义，仅用于数据库内部索引、关联';
COMMENT ON COLUMN t_order_info.order_id IS '业务主键，UUID v7原生类型，全局唯一，用于订单业务交互、API接口标识';
COMMENT ON COLUMN t_order_info.user_id IS '逻辑外键，关联用户表t_user_info的user_id，标识订单所属用户';
COMMENT ON COLUMN t_order_info.product_id IS '逻辑外键，关联商品表t_product_info的product_id，标识订单关联商品';
COMMENT ON COLUMN t_order_info.order_no IS '订单编号，业务唯一，用于用户查询、订单对账，不可为空';
COMMENT ON COLUMN t_order_info.amount IS '订单金额，DECIMAL(18,2)类型，保留2位小数，不可为空';
COMMENT ON COLUMN t_order_info.order_status IS '订单状态：0-待支付 1-已支付 2-已取消 3-已完成 4-退款中 5-已退款';
COMMENT ON COLUMN t_order_info.pay_type IS '支付方式，可选：1-微信 2-支付宝 3-银行卡，未支付时可为空';
COMMENT ON COLUMN t_order_info.pay_time IS '支付时间，可选，支付成功后更新，未支付时可为空，北京时间';
COMMENT ON COLUMN t_order_info.create_time IS '审计字段，订单创建时间，默认当前北京时间';
COMMENT ON COLUMN t_order_info.update_time IS '审计字段，订单更新时间，默认当前北京时间，订单状态变更时同步更新';
COMMENT ON COLUMN t_order_info.create_by IS '审计字段，订单创建人，关联用户表t_user_info的user_id';
COMMENT ON COLUMN t_order_info.update_by IS '审计字段，订单更新人，关联用户表t_user_info的user_id';
COMMENT ON COLUMN t_order_info.delete_flag IS '审计字段，逻辑删除标记：0-未删除 1-已删除，默认未删除';
COMMENT ON CONSTRAINT pk_t_order_info_id ON t_order_info IS '订单表物理主键约束，唯一标识表中每条订单记录';
COMMENT ON CONSTRAINT uk_t_order_info_order_id ON t_order_info IS '订单表业务主键唯一约束，保证order_id全局唯一';
COMMENT ON CONSTRAINT ck_t_order_info_status ON t_order_info IS '订单状态检查约束，限制状态值仅可取值0、1、2、3、4、5';
```

### 3\.3 模板 3：商品表

```sql
CREATE TABLE t_product_info (
    id              BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_t_product_info_id PRIMARY KEY,
    product_id      UUID NOT NULL CONSTRAINT uk_t_product_info_product_id UNIQUE,
    
    -- 业务字段
    product_name    VARCHAR(128) NOT NULL,
    product_code    VARCHAR(64) NOT NULL UNIQUE,
    category_id     UUID,
    brand_id        UUID,
    price_sale      DECIMAL(18,2) NOT NULL,
    price_cost      DECIMAL(18,2) NOT NULL,
    stock_num       INT NOT NULL DEFAULT 0,
    status          SMALLINT NOT NULL DEFAULT 0,
    
    -- 审计字段
    create_time     TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_time     TIMESTAMPTZ NOT NULL DEFAULT now(),
    create_by       UUID,
    update_by       UUID,
    delete_flag     SMALLINT NOT NULL DEFAULT 0,
    
    -- 业务约束
    CONSTRAINT ck_t_product_info_status CHECK (status IN (0,1,2,3))
);

-- 业务索引
CREATE INDEX idx_t_product_info_category_id ON t_product_info(category_id);
CREATE INDEX idx_t_product_info_brand_id ON t_product_info(brand_id);
CREATE INDEX idx_t_product_info_status ON t_product_info(status);
CREATE INDEX idx_t_product_info_create_time ON t_product_info(create_time);

-- 完整注释
COMMENT ON TABLE t_product_info IS '商品信息表，存储系统所有商品的基础信息、价格、库存、分类关联信息';
COMMENT ON COLUMN t_product_info.id IS '物理主键，BIGINT自增，无业务含义，仅用于数据库内部索引、关联';
COMMENT ON COLUMN t_product_info.product_id IS '业务主键，UUID v7原生类型，全局唯一，用于商品业务交互、API接口标识';
COMMENT ON COLUMN t_product_info.product_name IS '商品名称，不可为空，用于商品展示、搜索';
COMMENT ON COLUMN t_product_info.product_code IS '商品编码，业务唯一，不可为空，用于库存管理、商品对账';
COMMENT ON COLUMN t_product_info.category_id IS '逻辑外键，关联商品分类表t_product_category的category_id，标识商品所属分类';
COMMENT ON COLUMN t_product_info.brand_id IS '逻辑外键，关联商品品牌表t_product_brand的brand_id，标识商品所属品牌';
COMMENT ON COLUMN t_product_info.price_sale IS '商品售价，DECIMAL(18,2)类型，保留2位小数，不可为空，面向用户展示';
COMMENT ON COLUMN t_product_info.price_cost IS '商品成本价，DECIMAL(18,2)类型，保留2位小数，不可为空，用于内部成本核算';
COMMENT ON COLUMN t_product_info.stock_num IS '商品库存数量，INT类型，默认0，不可为负，用于库存管控';
COMMENT ON COLUMN t_product_info.status IS '商品状态：0-未上架 1-销售中 2-下架 3-禁售，控制商品展示、售卖权限';
COMMENT ON COLUMN t_product_info.create_time IS '审计字段，商品创建时间，默认当前北京时间';
COMMENT ON COLUMN t_product_info.update_time IS '审计字段，商品更新时间，默认当前北京时间，商品信息变更时同步更新';
COMMENT ON COLUMN t_product_info.create_by IS '审计字段，商品创建人，关联用户表t_user_info的user_id';
COMMENT ON COLUMN t_product_info.update_by IS '审计字段，商品更新人，关联用户表t_user_info的user_id';
COMMENT ON COLUMN t_product_info.delete_flag IS '审计字段，逻辑删除标记：0-未删除 1-已删除，默认未删除';
COMMENT ON CONSTRAINT pk_t_product_info_id ON t_product_info IS '商品表物理主键约束，唯一标识表中每条商品记录';
COMMENT ON CONSTRAINT uk_t_product_info_product_id ON t_product_info IS '商品表业务主键唯一约束，保证product_id全局唯一';
COMMENT ON CONSTRAINT ck_t_product_info_status ON t_product_info IS '商品状态检查约束，限制状态值仅可取值0、1、2、3';
```

## 四、附加最佳实践

### 4\.1 逻辑删除统一规范（当确实使用逻辑删除标记时，意味着不应真正删除业务数据）

1. 严禁物理 `DELETE` 语句删除业务数据；

2. 统一使用 `delete\_flag = 1` 做逻辑删除；

3. 所有业务查询默认强制带条件：`where delete\_flag = 0`。

### 4\.2 update\_time 自动触发器（全局通用）

```sql
-- 通用更新时间赋值函数
CREATE OR REPLACE FUNCTION func_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 示例：给用户表绑定更新触发器，所有表可复用
CREATE TRIGGER trig_t_user_info_update
BEFORE UPDATE ON t_user_info
FOR EACH ROW EXECUTE FUNCTION func_update_time();
```

### 4\.3 UUID v7 规范

PostgreSQL 14/17 推荐使用 **UUID v7** 原生类型，时序有序、索引效率远高于随机 UUID，统一作为业务主键对外暴露。

## 五、精简要点总结

1. 初始化流程：**先建库（默认北京时间时区）→ 建业务 Schema → 再建业务表**；

2. 严格双主键架构：物理主键负责底层存储索引，UUID v7 负责业务全局唯一交互；

3. 禁用物理外键，逻辑关联字段必须建索引；

4. 审计字段**字段、类型、顺序、位置**跨项目统一风格；

5. 时间统一 `TIMESTAMPTZ` \+ 库级默认北京时间，无需应用层额外转换时区；

6. 所有表、字段、约束必须补充完整注释，便于维护、迁移、交接。



---



<u>欢迎补充建议！</u>
