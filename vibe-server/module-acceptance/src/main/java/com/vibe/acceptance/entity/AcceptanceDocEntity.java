package com.vibe.acceptance.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 竣工文档实体（acceptance_doc 表）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acceptance_doc")
@Schema(description = "竣工文档")
public class AcceptanceDocEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属验收任务ID")
    private Long taskId;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "文档类型 TOPOLOGY/DEVICE_LIST/CONFIG_BACKUP/TEST_REPORT/MAINTENANCE_MANUAL/OTHER")
    private String docType;

    @Schema(description = "文档名称")
    private String name;

    @Schema(description = "文档URL（MinIO objectName）")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文档版本")
    @TableField("doc_version")
    private String docVersion;

    @Schema(description = "上传人ID")
    private Long uploaderId;

    @Schema(description = "备注")
    private String remark;
}
