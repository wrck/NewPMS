package com.vibe.common.exception;

import com.vibe.common.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 参数校验异常（用于业务层显式抛出的入参校验失败）
 *
 * @author vibe
 */
@Getter
public class ParamValidationException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ParamValidationException(String message) {
        super(ResultCode.PARAM_INVALID, message);
    }

    public ParamValidationException(String field, String message) {
        super(ResultCode.PARAM_INVALID, field + ": " + message);
    }
}
