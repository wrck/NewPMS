package com.vibe.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户状态变更 DTO
 *
 * <p>字段类型为 Integer（1-启用 0-禁用），与前端 vibe-web 对齐。
 * Service 层负责将 1/0 转为 ACTIVE/DISABLED。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "用户状态变更")
public class SysUserStatusDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态 1-启用 0-禁用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
