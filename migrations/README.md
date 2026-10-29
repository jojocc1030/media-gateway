# 数据库迁移

本目录存放 PostgreSQL DDL 迁移脚本，沿用 magpie 的“按版本号顺序手工执行”方式（未引入 Flyway）。

## 约定

- 目录：`migrations/versions/`。
- 命名：`V{序号}__{说明}.sql`，序号从 1 开始递增，禁止修改已执行的脚本。
- 每个脚本必须幂等可重跑（`CREATE TABLE IF NOT EXISTS`、先 `DROP` 再建等策略按场景选择），
  或在脚本顶部注明“仅执行一次”。
- 脚本内必须包含：表/字段/约束注释、索引、逻辑删除字段、审计字段（固定顺序位于表末尾）。
- 禁止物理外键；关联字段使用逻辑外键并建立索引。
- 业务主键统一 `VARCHAR(64)`（UUIDv7，去连字符），物理主键 `BIGINT GENERATED ALWAYS AS IDENTITY`。

## 执行

```bash
# 本地 compose 环境
docker compose up -d postgres
docker compose exec -T postgres psql -U postgres -d media_gateway_db < migrations/versions/V1__init_schema.sql
```

## 版本记录

| 版本 | 说明 |
|---|---|
| V1 | 初始化：更新时间触发器函数 + 示例业务表 `t_demo_item` |
