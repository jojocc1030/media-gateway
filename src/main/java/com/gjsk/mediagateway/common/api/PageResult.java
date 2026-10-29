package com.gjsk.mediagateway.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应。
 *
 * <p>分页字段必须与 {@code items} 同级放在 {@code data} 内，不得放入 {@code extra}。</p>
 *
 * @param <T> 列表元素类型
 */
@Data
@Schema(description = "统一分页响应")
public class PageResult<T> {

    /** 当前页数据。 */
    @Schema(description = "当前页数据")
    private List<T> items;

    /** 当前页码，从 1 开始。 */
    @Schema(description = "当前页码", example = "1")
    private long page;

    /** 每页数量。 */
    @Schema(description = "每页数量", example = "20")
    private long pageSize;

    /** 总记录数。 */
    @Schema(description = "总记录数", example = "128")
    private long total;

    /**
     * 构造分页响应。
     *
     * @param items    当前页数据
     * @param page     当前页码
     * @param pageSize 每页数量
     * @param total    总记录数
     */
    public static <T> PageResult<T> of(List<T> items, long page, long pageSize, long total) {
        PageResult<T> result = new PageResult<>();
        result.setItems(items);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotal(total);
        return result;
    }

    /**
     * 由 MyBatis-Plus 分页结果转换出对外分页响应。
     *
     * @param source 分页查询结果
     * @param mapper 实体到 VO 的转换函数
     */
    public static <E, T> PageResult<T> of(IPage<E> source, Function<E, T> mapper) {
        List<T> items = source.getRecords().stream().map(mapper).toList();
        return of(items, source.getCurrent(), source.getSize(), source.getTotal());
    }
}
