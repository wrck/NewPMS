package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户角色分配 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "用户角色分配")
public class SysUserRoleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
