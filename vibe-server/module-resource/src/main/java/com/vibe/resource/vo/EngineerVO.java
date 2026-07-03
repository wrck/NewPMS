package com.vibe.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工程师视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "工程师信息")
public class EngineerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工程师ID")
    private Long id;

    @Schema(description = "关联用户ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "工号")
    private String employeeNo;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "所属区域")
    private String region;

    @Schema(description = "状态 ACTIVE/RESIGNED")
    private String status;

    @Schema(description = "入职日期")
    private LocalDate hireDate;

    @Schema(description = "技能标签（JSON 字符串）")
    private String skills;

    @Schema(description = "认证资质（JSON 字符串）")
    private String certifications;

    @Schema(description = "技能列表")
    private List<EngineerSkillVO> skillList;

    @Schema(description = "当前负荷（时段内任务数，由调度服务填充）")
    private Integer currentWorkload;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
