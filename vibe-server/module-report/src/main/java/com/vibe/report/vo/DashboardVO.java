package com.vibe.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工作台首页统一 VO
 *
 * <p>根据当前登录用户角色填充对应的子数据块，其余为 null。</p>
 * <ul>
 *   <li>DIRECTOR / SUPER_ADMIN → {@link #director}</li>
 *   <li>PM → {@link #pm}</li>
 *   <li>ENGINEER → {@link #engineer}</li>
 *   <li>AGENT_ADMIN / AGENT_ENGINEER → {@link #agent}</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "工作台首页")
public class DashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前用户主角色编码")
    private String role;

    @Schema(description = "当前用户姓名")
    private String realName;

    @Schema(description = "总监首页数据（角色 DIRECTOR/SUPER_ADMIN 时填充）")
    private DirectorDashboardVO director;

    @Schema(description = "PM 首页数据（角色 PM 时填充）")
    private PmDashboardVO pm;

    @Schema(description = "工程师首页数据（角色 ENGINEER 时填充）")
    private EngineerDashboardVO engineer;

    @Schema(description = "代理商首页数据（角色 AGENT_ADMIN/AGENT_ENGINEER 时填充）")
    private AgentDashboardVO agent;
}
