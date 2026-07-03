package com.vibe.agent.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 交付物查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "交付物查询")
public class OutsourceDeliverableQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "转包任务ID")
    private Long outsourceTaskId;

    @Schema(description = "交付物类型 PHOTO/TEST_RECORD/RECEIPT/CONFIG/OTHER")
    private String deliverableType;
}
