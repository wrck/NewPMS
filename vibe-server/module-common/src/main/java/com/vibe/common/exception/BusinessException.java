package com.vibe.common.exception;

import com.vibe.common.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 *
 * <p>所有业务逻辑中的可预期错误都应抛出此异常，
 * 由 {@link com.vibe.common.handler.GlobalExceptionHandler} 统一捕获。</p>
 *
 * @author vibe
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final int code;

    /** 错误消息 */
    private final String message;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /* ============ 常用快捷构造 ============ */

    public static BusinessException of(ResultCode resultCode) {
        return new BusinessException(resultCode);
    }

    public static BusinessException of(ResultCode resultCode, String message) {
        return new BusinessException(resultCode, message);
    }

    public static BusinessException of(int code, String message) {
        return new BusinessException(code, message);
    }

    public static BusinessException notFound(String resource) {
        return new BusinessException(ResultCode.NOT_FOUND, resource + " 不存在");
    }

    public static BusinessException stateNotAllowed(String message) {
        return new BusinessException(ResultCode.STATE_NOT_ALLOWED, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(ResultCode.BUSINESS_CONFLICT, message);
    }
}
