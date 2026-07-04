package com.vibe.collaboration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户验收签核 DTO
 *
 * <p>客户在 H5 端通过 token 查看验收任务及测试记录后提交签核结果。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "客户验收签核请求")
public class CustomerAcceptanceSignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "客户签核链接 token", required = true)
    @NotNull(message = "签核 token 不能为空")
    private String token;

    @Schema(description = "签核结果 PASS/CONDITIONAL_PASS/REJECT", required = true)
    @NotNull(message = "签核结果不能为空")
    private String result;

    @Schema(description = "签核意见")
    private String remark;

    @Schema(description = "签核人姓名（可由登录态自动填充）")
    private String signUser;
}
