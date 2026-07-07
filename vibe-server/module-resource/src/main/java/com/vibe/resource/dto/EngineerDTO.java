package com.vibe.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 工程师新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "工程师新增/编辑")
public class EngineerDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID（编辑时必填）")
    private Long id;

    @Schema(description = "关联用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "关联用户ID不能为空")
    private Long userId;

    @Schema(description = "工号")
    @Size(max = 64, message = "工号长度不能超过64")
    private String engineerNo;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64")
    private String name;

    @Schema(description = "手机号")
    @Size(max = 32, message = "手机号长度不能超过32")
    private String phone;

    @Schema(description = "邮箱")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "所属区域")
    @Size(max = 32, message = "区域长度不能超过32")
    private String region;

    @Schema(description = "状态 ACTIVE/ON_LEAVE/RESIGNED")
    private String status;

    @Schema(description = "入职日期")
    private LocalDate hireDate;

    @Schema(description = "技能标签（JSON 字符串）")
    private String skills;

    @Schema(description = "认证资质（JSON 字符串）")
    private String certifications;

    @Schema(description = "技能列表（与 skills JSON 同步维护）")
    private List<EngineerSkillDTO> skillList;
}
