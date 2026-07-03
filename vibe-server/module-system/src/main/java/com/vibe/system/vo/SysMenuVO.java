package com.vibe.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单视图对象（含子菜单树）
 *
 * @author vibe
 */
@Data
@Schema(description = "菜单信息")
public class SysMenuVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型 MENU-菜单 BUTTON-按钮")
    private String menuType;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "是否可见 1-是 0-否")
    private Integer visible;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子菜单")
    private List<SysMenuVO> children;
}
