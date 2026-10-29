package com.gjsk.mediagateway.module.demo.dto.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 示例记录视图对象。
 */
@Schema(description = "示例记录")
public record DemoItemVO(
        @Schema(description = "业务主键", example = "0192f0c4e5b67d8f9a10b2c3d4e5f607") String itemId,
        @Schema(description = "名称", example = "示例记录") String name,
        @Schema(description = "状态：0-停用 1-启用", example = "1") Integer status,
        @Schema(description = "备注", example = "用于演示新增业务模块") String remark,
        @Schema(description = "创建时间") Instant createTime,
        @Schema(description = "更新时间") Instant updateTime) {
}
