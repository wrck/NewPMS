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
    private String engineerNo;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "所属组织名称")
    private String orgName;

    @Schema(description = "所属区域")
    private String region;

    @Schema(description = "状态 ACTIVE/ON_LEAVE/RESIGNED")
    private String status;

    @Schema(description = "入职日期")
    private LocalDate joinedAt;

    @Schema(description = "技能标签（JSON 字符串）")
    private String skills;

    @Schema(description = "认证资质（JSON 字符串）")
    private String certifications;

    @Schema(description = "技能列表")
    private List<EngineerSkillVO> skillList;

    @Schema(description = "利用率（百分比）")
    private Integer utilization;

    @Schema(description = "进行中任务数")
    private Integer ongoingTaskCount;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
