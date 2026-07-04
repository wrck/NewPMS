package com.vibe.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 割接操作日志实体（cutover_execution_log 表）
 *
 * <p>不继承 BaseEntity：日志表只插入，不更新/删除，无 version/deleted 字段。</p>
 *
 * @author vibe
 */
@Data
@TableName("cutover_execution_log")
@Schema(description = "割接操作日志")
public class CutoverExecutionLogEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "所属割接方案ID")
    private Long planId;

    @Schema(description = "关联步骤ID")
    private Long stepId;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作动作")
    private String action;

    @Schema(description = "操作时间")
    private LocalDateTime logTime;

    @Schema(description = "操作内容")
    private String logContent;

    @Schema(description = "日志级别")
    private String logLevel;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
