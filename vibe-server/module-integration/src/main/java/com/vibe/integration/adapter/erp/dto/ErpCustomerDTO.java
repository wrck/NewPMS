package com.vibe.integration.adapter.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ERP 客户主数据 DTO
 *
 * <p>用于 {@code ErpCustomerFeignClient} 与 ERP 系统 {@code /customers} 接口对接，
 * 同步客户主数据到 integration_config 之外的客户实体。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "ERP 客户主数据")
public class ErpCustomerDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "ERP 客户主键")
    private Long customerId;

    @Schema(description = "客户编码（唯一）")
    private String customerCode;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户简称")
    private String shortName;

    @Schema(description = "客户类型（ENTERPRISE/GOVERNMENT/INDIVIDUAL）")
    private String customerType;

    @Schema(description = "客户行业")
    private String industry;

    @Schema(description = "所在省")
    private String province;

    @Schema(description = "所在市")
    private String city;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    private String contactEmail;

    @Schema(description = "客户等级（VIP/IMPORTANT/NORMAL）")
    private String customerLevel;

    @Schema(description = "客户状态（ACTIVE/INACTIVE/FROZEN）")
    private String status;

    @Schema(description = "ERP 中最后更新时间")
    private LocalDateTime lastModifiedAt;

    @Schema(description = "同步时间（本系统填充）")
    private LocalDateTime syncedAt;
}
