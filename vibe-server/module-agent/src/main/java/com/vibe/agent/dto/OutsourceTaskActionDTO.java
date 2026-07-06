package com.vibe.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 转包任务操作 DTO
 *
 * <p>用于代理商接单/拒绝、指派工程师、PM 退回等操作。</p>
 *
 * @author vibe
 */
@Data
@Schema(description = "转包任务操作")
public class OutsourceTaskActionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "代理商工程师ID（指派工程师时必填）")
    private Long agentEngineerId;

    @Schema(description = "退回/拒绝原因")
    @Size(max = 500, message = "退回/拒绝原因长度不能超过500")
    private String reason;
}
