package com.vibe.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代理商交付物视图对象
 *
 * @author vibe
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "代理商交付物信息")
public class OutsourceDeliverableVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "交付物ID")
    private Long id;

    @Schema(description = "转包任务ID")
    private Long outsourceTaskId;

    @Schema(description = "交付物类型 PHOTO/TEST_RECORD/RECEIPT/CONFIG/OTHER")
    private String deliverableType;

    @Schema(description = "文件地址")
    private String fileUrl;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
