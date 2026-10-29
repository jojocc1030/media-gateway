package com.gjsk.mediagateway.persistence;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 业务实体基类。
 *
 * <p>双主键设计：{@code id} 为物理主键（BIGINT 自增，仅数据库内部使用），
 * 业务主键由各实体提供 {@code xxx_id}（UUIDv7 字符串，对外暴露）。
 * 审计字段固定为 {@code create_time / update_time / create_by / update_by / delete_flag}。</p>
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 物理主键，BIGINT 自增，无业务含义。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updateTime;

    /** 创建人。 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 更新人。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 逻辑删除标记：0-未删除 1-已删除。 */
    @TableLogic
    private Integer deleteFlag;
}
