package com.gjsk.mediagateway.module.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gjsk.mediagateway.persistence.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 示例实体：演示实体规范（继承审计基类、双主键、逻辑删除）。
 *
 * <p>该模块不是业务功能，仅作为新增业务模块的复制起点。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_demo_item")
public class DemoItem extends BaseEntity {

    /** 业务主键，UUIDv7 字符串。 */
    private String itemId;

    /** 名称。 */
    private String name;

    /** 状态：0-停用 1-启用。 */
    private Integer status;

    /** 备注。 */
    private String remark;
}
