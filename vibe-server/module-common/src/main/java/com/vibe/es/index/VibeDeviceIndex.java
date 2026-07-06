package com.vibe.es.index;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备索引 POJO（Elasticsearch 文档）
 *
 * <p>对应 ES 索引 {@code vibe_device}，覆盖设备列表查询、SN 检索、状态统计。</p>
 *
 * <p>字段对应关系（MySQL → ES）：</p>
 * <ul>
 *   <li>id ← device_instance.id</li>
 *   <li>sn ← device_instance.serial_number</li>
 *   <li>modelName ← device_model.model_name（关联查询）</li>
 *   <li>projectId ← device_instance.project_id</li>
 *   <li>status ← device_instance.status</li>
 *   <li>warehouse ← warehouse.name（关联查询）</li>
 *   <li>region ← project.region（关联查询）</li>
 *   <li>installedAt ← device_instance.install_date</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "设备索引文档")
public class VibeDeviceIndex implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "序列号 SN")
    private String sn;

    @Schema(description = "MAC 地址")
    private String macAddress;

    @Schema(description = "设备型号名称")
    private String modelName;

    @Schema(description = "所属项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "设备状态")
    private String status;

    @Schema(description = "仓库名称")
    private String warehouse;

    @Schema(description = "区域")
    private String region;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "安装时间")
    private LocalDateTime installedAt;
}
