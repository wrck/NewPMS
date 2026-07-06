package com.vibe.collaboration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 客户会话实体（customer_session）
 *
 * <p>记录客户登录会话信息，用于客户登录态管理与强制下线能力。
 * 删除会话即等同于强制下线（token 失效）。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_session")
@Schema(description = "客户会话")
public class CustomerSessionEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "登录 token")
    private String loginToken;

    @Schema(description = "登录 IP")
    private String loginIp;

    @Schema(description = "登录地点")
    private String loginLocation;

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "会话状态 ACTIVE/EXPIRED/REVOKED")
    private String status;
}
