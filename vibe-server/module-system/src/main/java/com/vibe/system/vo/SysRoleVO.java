package com.vibe.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图对象（含菜单ID列表）
 *
 * @author vibe
 */
@Data
@Schema(description = "角色信息")
public class SysRoleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "数据权限范围 ALL/DEPT/SELF/CUSTOM")
    private String dataScope;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "关联菜单ID列表")
    private List<Long> menuIds;
}
