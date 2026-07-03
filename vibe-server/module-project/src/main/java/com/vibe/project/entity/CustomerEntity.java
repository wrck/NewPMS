package com.vibe.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 客户实体（customer）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
@Schema(description = "客户")
public class CustomerEntity extends ProjectBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

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
}
