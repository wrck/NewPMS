package com.vibe.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 统一响应体
 *
 * <pre>
 * 成功响应：
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ... },
 *   "timestamp": 1719900000000
 * }
 *
 * 错误响应：
 * {
 *   "code": 40001,
 *   "message": "项目名称不能为空",
 *   "data": null,
 *   "timestamp": 1719900000000
 * }
 * </pre>
 *
 * @param <T> 数据载荷类型
 * @author vibe
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "统一响应体")
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 成功状态码 */
    public static final int CODE_SUCCESS = 200;

    @Schema(description = "业务状态码：200 成功，其它为错误码", example = "200")
    private int code;

    @Schema(description = "提示信息", example = "success")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "时间戳（毫秒）", example = "1719900000000")
    private long timestamp;

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toEpochMilli();
    }

    /* ============ 快捷构造方法 ============ */

    public static <T> Result<T> success() {
        return new Result<>(CODE_SUCCESS, ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(CODE_SUCCESS, ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(CODE_SUCCESS, message, data);
    }

    public static <T> Result<T> successMessage(String message) {
        return new Result<>(CODE_SUCCESS, message, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, T data) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), data);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code == CODE_SUCCESS;
    }

    /**
     * 设置 timestamp（用于全局异常处理器统一赋值）
     */
    public Result<T> withTimestamp(long ts) {
        this.timestamp = ts;
        return this;
    }
}
