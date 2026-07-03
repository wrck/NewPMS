package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色新增/编辑 DTO
 *
 * <p>JSON 字段命名与前端 vibe-web 的 {@code SysRoleDTO} 类型对齐：
 * <ul>
 *   <li>{@code permissionCodes} —— 权限标识列表（前端字段名，Java 字段仍为 {@code menuIds}）</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "角色新增/编辑")
public class SysRoleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID（编辑时必填）")
    private Long id;

    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64")
    private String roleName;

    @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码长度不能超过64")
    private String roleCode;

    @Schema(description = "描述")
    @Size(max = 255, message = "描述长度不能超过255")
    private String description;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "数据权限范围 ALL/DEPT/SELF/CUSTOM")
    private String dataScope;

    /**
     * 权限标识列表（前端字段名）。
     * <p>前端传字符串数组，Service 层负责转换为菜单ID列表。
     * 支持两种格式：菜单ID的字符串形式（如 ["1","2"]）或权限标识（如 ["system:user"]）。</p>
     */
    @Schema(description = "权限标识列表")
    private List<String> permissionCodes;
}
