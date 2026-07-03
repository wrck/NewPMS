package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜单新增/编辑 DTO
 *
 * @author vibe
 */
@Data
@Schema(description = "菜单新增/编辑")
public class SysMenuDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID（编辑时必填）")
    private Long id;

    @Schema(description = "父菜单ID（0为根）")
    private Long parentId;

    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 64, message = "菜单名称长度不能超过64")
    private String menuName;

    @Schema(description = "菜单类型 MENU-菜单 BUTTON-按钮", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    @Schema(description = "路由路径")
    @Size(max = 128, message = "路由路径长度不能超过128")
    private String path;

    @Schema(description = "前端组件路径")
    @Size(max = 128, message = "前端组件路径长度不能超过128")
    private String component;

    @Schema(description = "权限标识")
    @Size(max = 128, message = "权限标识长度不能超过128")
    private String perms;

    @Schema(description = "图标")
    @Size(max = 64, message = "图标长度不能超过64")
    private String icon;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "是否可见 1-是 0-否")
    private Integer visible;
}
