package com.vibe.common.exception;

import com.vibe.common.result.ResultCode;

import java.io.Serial;

/**
 * 数据异常
 *
 * <p>用于业务层显式抛出的数据不存在 / 数据状态非法 / 数据冲突 / 数据完整性破坏等场景。
 * 错误码区间：422xx（HTTP 422 Unprocessable Entity 语义）。</p>
 *
 * <p>由 {@link com.vibe.common.handler.GlobalExceptionHandler} 统一捕获并返回 422xx 错误码。</p>
 *
 * @author vibe
 */
public class DataException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 默认数据异常错误码（42200） */
    private static final int DEFAULT_DATA_ERROR_CODE = 42200;

    public DataException(String message) {
        super(DEFAULT_DATA_ERROR_CODE, message);
    }

    public DataException(int code, String message) {
        super(code, message);
    }

    public DataException(String message, Throwable cause) {
        super(DEFAULT_DATA_ERROR_CODE, message, cause);
    }
}
