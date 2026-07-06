package com.vibe.lowcode.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.vibe.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 低代码列表配置实体（lowcode_list_config 表）
 *
 * <p>存储列表的 JSON Schema（Draft 7），定义列定义/筛选条件/操作按钮，
 * 运行时由 ListRenderer 解析并渲染为动态列表。</p>
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lowcode_list_config")
@Schema(description = "低代码列表配置")
public class LowcodeListConfigEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "配置编码（唯一）")
    private String configCode;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "JSON Schema（列定义/筛选/操作按钮）")
    private String schemaJson;

    @Schema(description = "关联模板ID（lowcode_template.id，可空）")
    private Long templateId;

    // version 字段由 BaseEntity 继承（@Version 乐观锁 + 版本追踪，映射 version 列）

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建人ID")
    private Long creatorId;
}
