package com.vibe.common.exception;

import com.vibe.common.result.ResultCode;

import java.io.Serial;

/**
 * 系统异常
 *
 * <p>用于业务层抛出的非预期系统级错误，例如数据库连接失败、缓存异常、文件 IO 错误、
 * 网络错误、配置错误等场景。区别于 {@link BusinessException} 的可预期业务错误，
 * 系统异常通常需要触发告警与运维介入。</p>
 *
 * <p>错误码区间：500xx（{@link ResultCode#INTERNAL_ERROR} 等）。
 * 由 {@link com.vibe.common.handler.GlobalExceptionHandler} 统一捕获并触发告警。</p>
 *
 * @author vibe
 */
public class SystemException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SystemException(String message) {
        super(ResultCode.INTERNAL_ERROR, message);
    }

    public SystemException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public SystemException(String message, Throwable cause) {
        super(ResultCode.INTERNAL_ERROR.getCode(), message, cause);
    }
}
