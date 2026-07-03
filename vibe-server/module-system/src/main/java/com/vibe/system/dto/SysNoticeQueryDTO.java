package com.vibe.system.dto;

import com.vibe.common.base.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 站内信分页查询 DTO
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "站内信分页查询")
public class SysNoticeQueryDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "通知类型 1-通知 2-消息")
    private Integer noticeType;

    @Schema(description = "已读状态 0-未读 1-已读")
    private Integer readStatus;

    @Schema(description = "通知标题（模糊）")
    private String keyword;
}
