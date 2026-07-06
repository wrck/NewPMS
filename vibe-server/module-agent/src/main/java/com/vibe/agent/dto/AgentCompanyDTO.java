package com.vibe.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 代理商公司新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "代理商公司新增/编辑")
public class AgentCompanyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "公司ID（编辑时必填）")
    private Long id;

    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公司名称不能为空")
    @Size(max = 128, message = "公司名称长度不能超过128")
    private String companyName;

    @Schema(description = "公司编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公司编码不能为空")
    @Size(max = 64, message = "公司编码长度不能超过64")
    private String companyCode;

    @Schema(description = "资质等级")
    @Size(max = 64, message = "资质等级长度不能超过64")
    private String qualification;

    @Schema(description = "联系人")
    @Size(max = 64, message = "联系人长度不能超过64")
    private String contactName;

    @Schema(description = "联系电话")
    @Size(max = 32, message = "联系电话长度不能超过32")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确（需为 11 位手机号）")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    @Size(max = 128, message = "联系邮箱长度不能超过128")
    @Email(message = "联系邮箱格式不正确")
    private String contactEmail;

    @Schema(description = "公司地址")
    @Size(max = 255, message = "公司地址长度不能超过255")
    private String address;

    @Schema(description = "服务区域列表")
    private List<String> serviceRegions;

    @Schema(description = "服务产品线列表")
    private List<String> productLines;

    @Schema(description = "状态 ACTIVE/SUSPENDED/TERMINATED")
    private String status;

    @Schema(description = "合作开始日期")
    private LocalDate cooperationStart;

    @Schema(description = "合作结束日期")
    private LocalDate cooperationEnd;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
