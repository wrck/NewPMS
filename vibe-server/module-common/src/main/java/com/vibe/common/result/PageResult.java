package com.vibe.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应体
 *
 * <pre>
 * {
 *   "records": [ ... ],
 *   "total": 156,
 *   "page": 1,
 *   "size": 20,
 *   "pages": 8
 * }
 * </pre>
 *
 * @param <T> 列表元素类型
 * @author vibe
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应体")
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "总记录数", example = "156")
    private long total;

    @Schema(description = "当前页码（从 1 开始）", example = "1")
    private long page;

    @Schema(description = "每页大小", example = "20")
    private long size;

    @Schema(description = "总页数", example = "8")
    private long pages;

    /* ============ 快捷构造方法 ============ */

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size, 0L);
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        long pages = size > 0 ? (total + size - 1) / size : 0L;
        return new PageResult<>(records, total, page, size, pages);
    }

    /**
     * 从 MyBatis-Plus IPage 构造（避免在 common 引入过多耦合，使用反射式方法签名）。
     * 业务模块可直接调用 {@link #of(List, long, long, long)}。
     */
    public static <T> PageResult<T> of(List<T> records, long total, long page, long size, long pages) {
        return new PageResult<>(records, total, page, size, pages);
    }

    /**
     * 列表元素类型转换（Entity VO 转换）
     */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mapped = records == null
                ? Collections.emptyList()
                : records.stream().map(mapper).collect(Collectors.toList());
        return new PageResult<>(mapped, this.total, this.page, this.size, this.pages);
    }
}
