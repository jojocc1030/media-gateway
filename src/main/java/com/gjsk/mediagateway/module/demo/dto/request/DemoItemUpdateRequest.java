package com.gjsk.mediagateway.module.demo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新示例记录请求。
 */
@Schema(description = "更新示例记录请求")
public record DemoItemUpdateRequest(
        @Schema(description = "名称", example = "示例记录（更新）")
        @NotBlank(message = "名称不能为空")
        @Size(max = 128, message = "名称长度不能超过 128")
        String name,

        @Schema(description = "状态：0-停用 1-启用", example = "1")
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态只能为 0 或 1")
        @Max(value = 1, message = "状态只能为 0 或 1")
        Integer status,

        @Schema(description = "备注", example = "更新后的备注")
        @Size(max = 512, message = "备注长度不能超过 512")
        String remark) {
}
