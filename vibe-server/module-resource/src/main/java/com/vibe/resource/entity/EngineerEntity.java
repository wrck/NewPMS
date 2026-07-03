package com.vibe.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 工程师资源池实体（engineer）
 *
 * <p>关联 sys_user.user_id，存储工程师档案、技能标签 JSON、认证资质 JSON、区域、在职状态等。
 * 表本身无 version 字段，但因继承 {@link BaseEntity} 携带 @Version 字段，
 * 已通过 schema 调整保持一致——如该表无乐观锁需求，可改为继承
 * {@link ResourceBaseEntity}。当前实现按"工程师档案属于核心资源数据"保留乐观锁。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("engineer")
@Schema(description = "工程师资源池")
public class EngineerEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关联用户ID（sys_user.id）")
    private Long userId;

    @Schema(description = "工号")
    private String employeeNo;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "所属区域")
    private String region;

    @Schema(description = "状态 ACTIVE-在职 RESIGNED-离职")
    private String status;

    @Schema(description = "入职日期")
    private LocalDate hireDate;

    @Schema(description = "技能标签（JSON 字符串：产品线+等级）")
    private String skills;

    @Schema(description = "认证资质（JSON 字符串：HCIE/HCIP/CCIE 等）")
    private String certifications;
}
