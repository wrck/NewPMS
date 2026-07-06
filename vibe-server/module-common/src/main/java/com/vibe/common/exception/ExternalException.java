package com.vibe.common.exception;

import com.vibe.common.result.ResultCode;

import java.io.Serial;

/**
 * 外部依赖异常
 *
 * <p>用于业务层调用外部系统失败时抛出，例如 ERP 同步失败、IM 推送失败、物流查询失败、
 * OA 审批联动失败、MinIO 文件服务异常等场景。</p>
 *
 * <p>错误码区间：502xx（{@link ResultCode#EXTERNAL_SERVICE_ERROR} 等）。
 * 由 {@link com.vibe.common.handler.GlobalExceptionHandler} 统一捕获并触发告警。</p>
 *
 * @author vibe
 */
public class ExternalException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExternalException(String message) {
        super(ResultCode.EXTERNAL_SERVICE_ERROR, message);
    }

    public ExternalException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public ExternalException(String message, Throwable cause) {
        super(ResultCode.EXTERNAL_SERVICE_ERROR.getCode(), message, cause);
    }
}
