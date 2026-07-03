package com.vibe.common.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询基类
 *
 * <p>所有列表查询接口的 DTO 应继承此类，统一获取 page/size 参数。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "分页查询基类")
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "页码，从 1 开始", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "排序字段（可多，逗号分隔）", example = "create_time")
    private String sortField;

    @Schema(description = "排序方向：ASC/DESC", example = "DESC")
    private String sortOrder = "DESC";

    public long offset() {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : size;
        return (long) (p - 1) * s;
    }

    public long limit() {
        return size == null || size < 1 ? 20L : size.longValue();
    }
}
