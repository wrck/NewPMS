package com.vibe.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 代理商交付物实体（outsource_deliverable）
 *
 * <p>对应 schema.sql 中的 outsource_deliverable 表，存储代理商提交的施工照片、
 * 测试记录、签收单等交付物。deliverable_type 取值 PHOTO/TEST_RECORD/RECEIPT/CONFIG/OTHER。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("outsource_deliverable")
@Schema(description = "代理商交付物")
public class OutsourceDeliverableEntity extends AgentBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 转包任务ID */
    @Schema(description = "转包任务ID")
    private Long outsourceTaskId;

    /** 交付物类型 PHOTO/TEST_RECORD/RECEIPT/CONFIG/OTHER */
    @Schema(description = "交付物类型")
    private String deliverableType;

    /** 文件地址 */
    @Schema(description = "文件地址")
    private String fileUrl;

    /** 文件名 */
    @Schema(description = "文件名")
    private String fileName;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
