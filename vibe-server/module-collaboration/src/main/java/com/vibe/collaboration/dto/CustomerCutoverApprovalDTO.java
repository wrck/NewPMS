package com.vibe.collaboration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户割接审批 DTO
 *
 * <p>客户在 H5 端通过 token 查看割接方案后提交审批结果。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "客户割接审批请求")
public class CustomerCutoverApprovalDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "客户签核链接 token", required = true, example = "debd477f09bf46a8945b9c6b1ead5792")
    @NotNull(message = "签核 token 不能为空")
    private String token;

    @Schema(description = "审批结果 APPROVED/REJECTED", required = true)
    @NotNull(message = "审批结果不能为空")
    private String result;

    @Schema(description = "审批意见")
    private String remark;

    @Schema(description = "签核人姓名（可由登录态自动填充，留作预留）")
    private String signUser;
}
