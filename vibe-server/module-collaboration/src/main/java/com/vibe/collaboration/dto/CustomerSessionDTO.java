package com.vibe.collaboration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户会话新增/编辑 DTO
 *
 * <p>会话主要由登录流程创建，本 DTO 供内部调用与运维管理使用。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "客户会话新增/编辑")
public class CustomerSessionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID（编辑时必填）")
    private Long id;

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long customerId;

    @Schema(description = "登录 token", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登录 token 不能为空")
    @Size(max = 255, message = "登录 token 长度不能超过255")
    private String loginToken;

    @Schema(description = "登录 IP")
    @Size(max = 64, message = "登录 IP 长度不能超过64")
    private String loginIp;

    @Schema(description = "登录地点")
    @Size(max = 128, message = "登录地点长度不能超过128")
    private String loginLocation;

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "会话状态 ACTIVE/EXPIRED/REVOKED")
    private String status;
}
