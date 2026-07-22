package com.vibe.acceptance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 竣工文档 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "竣工文档")
public class AcceptanceDocVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "所属验收任务ID")
    private Long taskId;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "文档类型")
    private String docType;

    @Schema(description = "文档名称")
    private String name;

    @Schema(description = "文档URL")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文档版本")
    private String docVersion;

    @Schema(description = "上传人ID")
    private Long uploaderId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
