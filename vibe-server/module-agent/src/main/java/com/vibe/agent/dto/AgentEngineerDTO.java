package com.vibe.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 代理商工程师新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "代理商工程师新增/编辑")
public class AgentEngineerDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID（编辑时必填）")
    private Long id;

    @Schema(description = "所属代理商ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "所属代理商ID不能为空")
    private Long agentCompanyId;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64")
    private String name;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    @Size(max = 32, message = "手机号长度不能超过32")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确（需为 11 位手机号）")
    private String phone;

    @Schema(description = "邮箱")
    @Size(max = 128, message = "邮箱长度不能超过128")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "技能标签列表")
    private List<String> skills;

    @Schema(description = "认证资质列表")
    private List<String> certifications;

    @Schema(description = "状态 ACTIVE/DISABLED")
    private String status;
}
