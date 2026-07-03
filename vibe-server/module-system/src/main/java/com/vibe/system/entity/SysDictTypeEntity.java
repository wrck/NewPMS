package com.vibe.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 字典类型实体（sys_dict_type）
 *
 * @author vibe
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
@Schema(description = "字典类型")
public class SysDictTypeEntity extends SysBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典类型编码")
    private String dictType;

    @Schema(description = "状态 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
