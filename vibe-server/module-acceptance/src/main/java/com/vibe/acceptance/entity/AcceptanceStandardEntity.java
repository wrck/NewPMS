package com.vibe.acceptance.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 验收标准模板实体（acceptance_standard 表）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acceptance_standard")
@Schema(description = "验收标准模板")
public class AcceptanceStandardEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标准名称")
    private String name;

    @Schema(description = "适用项目类型")
    private String projectType;

    @Schema(description = "标准版本")
    @TableField("standard_version")
    private String standardVersion;

    @Schema(description = "标准说明")
    private String description;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;
}
