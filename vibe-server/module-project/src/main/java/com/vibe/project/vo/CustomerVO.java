package com.vibe.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户视图对象
 *
 * @author vibe
 */
@Data
@Schema(description = "客户信息")
public class CustomerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "客户ID")
    private Long id;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户编码")
    private String customerCode;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    private String contactEmail;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
