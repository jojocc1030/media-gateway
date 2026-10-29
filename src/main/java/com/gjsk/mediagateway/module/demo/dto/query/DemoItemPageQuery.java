package com.gjsk.mediagateway.module.demo.dto.query;

import com.gjsk.mediagateway.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 示例记录分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "示例记录分页查询参数")
public class DemoItemPageQuery extends PageQuery {

    /** 名称模糊搜索。 */
    @Schema(description = "名称模糊搜索", example = "示例")
    private String name;

    /** 状态精确筛选。 */
    @Schema(description = "状态：0-停用 1-启用", example = "1")
    private Integer status;
}
