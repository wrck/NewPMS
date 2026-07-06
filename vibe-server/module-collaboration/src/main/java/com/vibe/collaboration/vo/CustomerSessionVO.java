package com.vibe.collaboration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户会话视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "客户会话")
public class CustomerSessionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "登录 token（脱敏）")
    private String loginToken;

    @Schema(description = "登录 IP")
    private String loginIp;

    @Schema(description = "登录地点")
    private String loginLocation;

    @Schema(description = "登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @Schema(description = "会话状态 ACTIVE/EXPIRED/REVOKED")
    private String status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
