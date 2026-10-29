package com.gjsk.mediagateway.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 通用分页查询参数基类。
 *
 * <p>接口层使用聚合查询 DTO 时继承本类；直接以 query 传参的接口按 snake_case 显式绑定
 * {@code page} 与 {@code page_size}。</p>
 */
@Data
@Schema(description = "通用分页参数")
public class PageQuery {

    /** 默认页码。 */
    public static final int DEFAULT_PAGE = 1;
    /** 默认每页数量。 */
    public static final int DEFAULT_PAGE_SIZE = 20;
    /** 每页数量上限。 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 页码，从 1 开始。 */
    @Min(value = 1, message = "页码不能小于 1")
    @Schema(description = "页码，从 1 开始", example = "1")
    private Integer page = DEFAULT_PAGE;

    /** 每页数量，最大 100。 */
    @Min(value = 1, message = "每页数量不能小于 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页数量不能超过 100")
    @Schema(description = "每页数量，最大 100", example = "20")
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    /**
     * 收敛非法分页参数，避免越界查询拖垮数据库。
     *
     * <p>页码小于 1 收敛为 1；每页数量小于 1 收敛为 1，大于 100 收敛为 100。</p>
     */
    public void normalize() {
        if (page == null || page < 1) {
            page = DEFAULT_PAGE;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
    }

    /**
     * 规范化后的页码。
     */
    public int currentPage() {
        return (page == null || page < 1) ? DEFAULT_PAGE : page;
    }

    /**
     * 规范化后的每页数量。
     */
    public int currentPageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 数据库查询偏移量。
     */
    public long offset() {
        return (long) (currentPage() - 1) * currentPageSize();
    }
}
