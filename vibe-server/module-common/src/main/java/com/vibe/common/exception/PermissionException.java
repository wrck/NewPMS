package com.vibe.common.exception;

import com.vibe.common.result.ResultCode;

import java.io.Serial;

/**
 * 权限异常
 *
 * <p>用于业务层显式抛出的权限不足 / 数据权限越权 / 操作权限缺失等场景。
 * 错误码区间：403xx（{@link ResultCode#FORBIDDEN} 等）。</p>
 *
 * <p>由 {@link com.vibe.common.handler.GlobalExceptionHandler} 统一捕获并返回 403xx 错误码。</p>
 *
 * @author vibe
 */
public class PermissionException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PermissionException(String message) {
        super(ResultCode.FORBIDDEN, message);
    }

    public PermissionException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public PermissionException(ResultCode resultCode, String message, Throwable cause) {
        super(resultCode.getCode(), message, cause);
    }
}
