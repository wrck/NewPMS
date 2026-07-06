package com.vibe.es.index;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目索引 POJO（Elasticsearch 文档）
 *
 * <p>对应 ES 索引 {@code vibe_project}，覆盖项目列表查询与全文检索场景。</p>
 *
 * <p>字段对应关系（MySQL → ES）：</p>
 * <ul>
 *   <li>id ← project.id</li>
 *   <li>name ← project.project_name</li>
 *   <li>customerName ← project.customer_name（关联查询）</li>
 *   <li>productLine ← project.product_line</li>
 *   <li>region ← project.region</li>
 *   <li>status ← project.status</li>
 *   <li>pmId ← project.pm_id</li>
 *   <li>phase ← project.current_phase</li>
 *   <li>createdAt ← project.create_time</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "项目索引文档")
public class VibeProjectIndex implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private Long id;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "产品线")
    private String productLine;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "项目状态")
    private String status;

    @Schema(description = "项目经理ID")
    private Long pmId;

    @Schema(description = "当前阶段编码")
    private String phase;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
