package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色菜单分配 DTO
 *
 * <p>JSON 字段名 {@code permissionCodes} 与前端 vibe-web 的
 * {@code assignRolePermissions(id, permissionCodes)} 调用对齐。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "角色菜单分配")
public class SysRoleMenuDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "权限标识列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "权限标识列表不能为空")
    private List<String> permissionCodes;
}
