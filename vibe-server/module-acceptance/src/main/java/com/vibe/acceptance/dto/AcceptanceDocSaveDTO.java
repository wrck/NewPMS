package com.vibe.acceptance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 竣工文档创建 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "竣工文档创建")
public class AcceptanceDocSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键（更新时必填）")
    private Long id;

    @NotNull(message = "验收任务ID不能为空")
    @Schema(description = "所属验收任务ID")
    private Long taskId;

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "关联项目ID")
    private Long projectId;

    @NotBlank(message = "文档类型不能为空")
    @Schema(description = "文档类型 TOPOLOGY/DEVICE_LIST/CONFIG_BACKUP/TEST_REPORT/MAINTENANCE_MANUAL/OTHER")
    private String docType;

    @NotBlank(message = "文档名称不能为空")
    @Schema(description = "文档名称")
    private String name;

    @NotBlank(message = "文档URL不能为空")
    @Schema(description = "文档URL（MinIO objectName）")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文档版本")
    private String docVersion;

    @Schema(description = "备注")
    private String remark;
}
