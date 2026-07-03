package com.vibe.collaboration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目可下载文档 VO
 *
 * @author vibe
 */
@Data
@Schema(description = "项目可下载文档")
public class DocumentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID（合成主键：阶段ID + 序号）")
    private String docId;

    @Schema(description = "文档名称")
    private String docName;

    @Schema(description = "文档类型 DESIGN/REPORT/CONFIG/OTHER")
    private String docType;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;

    @Schema(description = "预签名下载 URL（限时有效）")
    private String downloadUrl;
}
