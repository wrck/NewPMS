package com.vibe.common.handler;

import com.vibe.common.exception.BusinessException;
import com.vibe.common.exception.DataException;
import com.vibe.common.exception.ExternalException;
import com.vibe.common.exception.PermissionException;
import com.vibe.common.exception.SystemException;
import com.vibe.common.result.Result;
import com.vibe.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>统一捕获 Controller 层抛出的异常，按 {@link ResultCode} 规范返回统一响应体。
 * 所有响应均包含 {@code traceId}，便于通过 ELK / Zipkin 检索完整调用链。</p>
 *
 * <h3>异常分级与错误码区间</h3>
 * <ul>
 *   <li>{@link BusinessException}        → 400xx 业务异常</li>
 *   <li>{@link PermissionException}       → 403xx 权限异常</li>
 *   <li>{@link DataException}             → 422xx 数据异常</li>
 *   <li>{@link ExternalException}         → 502xx 外部依赖异常（触发告警）</li>
 *   <li>{@link SystemException}           → 500xx 系统异常（触发告警）</li>
 *   <li>{@link MethodArgumentNotValidException} → 40001 参数校验错误 + errors 数组</li>
 *   <li>{@link ConstraintViolationException} → 40002 约束校验错误</li>
 *   <li>{@link AccessDeniedException}     → 403xx 权限不足</li>
 *   <li>{@link NoHandlerFoundException}   → 404xx 资源不存在</li>
 *   <li>{@link Exception}                 → 50000 兜底系统错误</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ============ 业务异常（400xx）============ */

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("[业务异常] {} {} -> code={}, msg={}", request.getMethod(), request.getRequestURI(),
                ex.getCode(), ex.getMessage());
        return buildFailResult(ex.getCode(), ex.getMessage());
    }

    /* ============ 权限异常（403xx）============ */

    @ExceptionHandler(PermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handlePermissionException(PermissionException ex, HttpServletRequest request) {
        log.warn("[权限异常] {} {} -> code={}, msg={}", request.getMethod(), request.getRequestURI(),
                ex.getCode(), ex.getMessage());
        return buildFailResult(ex.getCode(), ex.getMessage());
    }

    /* ============ 数据异常（422xx）============ */

    @ExceptionHandler(DataException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Result<Void> handleDataException(DataException ex, HttpServletRequest request) {
        log.warn("[数据异常] {} {} -> code={}, msg={}", request.getMethod(), request.getRequestURI(),
                ex.getCode(), ex.getMessage());
        return buildFailResult(ex.getCode(), ex.getMessage());
    }

    /* ============ 外部依赖异常（502xx，触发告警）============ */

    @ExceptionHandler(ExternalException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Result<Void> handleExternalException(ExternalException ex, HttpServletRequest request) {
        // 外部依赖异常通常表示 ERP/IM/物流/OA 等第三方系统不可用，需触发告警
        log.error("[外部依赖异常] {} {} -> code={}, msg={}", request.getMethod(), request.getRequestURI(),
                ex.getCode(), ex.getMessage(), ex);
        triggerAlert("EXTERNAL", ex.getCode(), ex.getMessage(), request);
        return buildFailResult(ex.getCode(), "外部服务调用失败，请稍后重试");
    }

    /* ============ 系统异常（500xx，触发告警）============ */

    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSystemException(SystemException ex, HttpServletRequest request) {
        // 系统异常通常表示数据库/缓存/文件 IO/网络等基础设施故障，需触发告警
        log.error("[系统异常] {} {} -> code={}, msg={}", request.getMethod(), request.getRequestURI(),
                ex.getCode(), ex.getMessage(), ex);
        triggerAlert("SYSTEM", ex.getCode(), ex.getMessage(), request);
        return buildFailResult(ex.getCode(), "系统内部错误，请联系管理员");
    }

    /* ============ 参数校验异常：@Valid RequestBody（40001 + errors 数组）============ */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<FieldErrorItem>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                     HttpServletRequest request) {
        List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        String msg = buildErrorMessage(errors);
        log.warn("[参数校验失败] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        // 错误码 40001：参数校验失败
        Result<List<FieldErrorItem>> result = Result.<List<FieldErrorItem>>fail(40001, msg).refreshTraceId();
        result.setData(errors);
        return result;
    }

    /* ============ 参数校验异常：表单绑定（40001 + errors 数组）============ */

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<FieldErrorItem>> handleBindException(BindException ex, HttpServletRequest request) {
        List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        String msg = buildErrorMessage(errors);
        log.warn("[参数绑定失败] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        // 错误码 40001：参数校验失败
        Result<List<FieldErrorItem>> result = Result.<List<FieldErrorItem>>fail(40001, msg).refreshTraceId();
        result.setData(errors);
        return result;
    }

    /* ============ 参数校验异常：@Validated 路径/查询参数（40002）============ */

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<FieldErrorItem>> handleConstraintViolation(ConstraintViolationException ex,
                                                                  HttpServletRequest request) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(cv -> new FieldErrorItem(extractField(cv), cv.getMessage()))
                .collect(Collectors.toList());
        String msg = buildErrorMessage(errors);
        log.warn("[约束校验失败] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        // 错误码 40002：约束校验失败
        Result<List<FieldErrorItem>> result = Result.<List<FieldErrorItem>>fail(40002, msg).refreshTraceId();
        result.setData(errors);
        return result;
    }

    /* ============ 必填参数缺失 ============ */

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String msg = "缺少必填参数: " + ex.getParameterName();
        log.warn("[参数缺失] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        return buildFailResult(ResultCode.PARAM_MISSING, msg);
    }

    /* ============ 参数类型不匹配 ============ */

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String msg = "参数 " + ex.getName() + " 类型不匹配";
        log.warn("[参数类型不匹配] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        return buildFailResult(ResultCode.PARAM_TYPE_MISMATCH, msg);
    }

    /* ============ 请求体不可读/JSON 格式错误 ============ */

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("[请求体格式错误] {} {}", request.getMethod(), request.getRequestURI());
        return buildFailResult(ResultCode.HTTP_MESSAGE_NOT_READABLE);
    }

    /* ============ 请求方法不支持 ============ */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String msg = "请求方法不支持: " + ex.getMethod();
        log.warn("[方法不支持] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        return buildFailResult(ResultCode.PARAM_INVALID, msg);
    }

    /* ============ 权限不足（403xx）============ */

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[权限不足] {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildFailResult(ResultCode.FORBIDDEN);
    }

    /* ============ 资源不存在（404xx）============ */

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("[资源不存在] {} {}", request.getMethod(), request.getRequestURI());
        return buildFailResult(ResultCode.NOT_FOUND);
    }

    /* ============ 兜底：未知系统异常（50000）============ */

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex, HttpServletRequest request) {
        // 未知异常作为兜底，触发告警通知运维介入
        log.error("[系统异常] {} {}", request.getMethod(), request.getRequestURI(), ex);
        triggerAlert("UNKNOWN", 50000, ex.getMessage(), request);
        return buildFailResult(ResultCode.INTERNAL_ERROR, "系统内部错误，请联系管理员");
    }

    /* ============ 辅助方法 ============ */

    /**
     * 构建失败响应，刷新 traceId（取当前 MDC 中的 traceId）并设置时间戳。
     */
    private Result<Void> buildFailResult(int code, String message) {
        return Result.<Void>fail(code, message)
                .refreshTraceId()
                .withTimestamp(Instant.now().toEpochMilli());
    }

    /**
     * 构建失败响应（基于 ResultCode），刷新 traceId 并设置时间戳。
     */
    private Result<Void> buildFailResult(ResultCode resultCode) {
        return Result.<Void>fail(resultCode)
                .refreshTraceId()
                .withTimestamp(Instant.now().toEpochMilli());
    }

    /**
     * 构建失败响应（基于 ResultCode + 自定义消息），刷新 traceId 并设置时间戳。
     */
    private Result<Void> buildFailResult(ResultCode resultCode, String message) {
        return Result.<Void>fail(resultCode, message)
                .refreshTraceId()
                .withTimestamp(Instant.now().toEpochMilli());
    }

    /**
     * 触发告警通知。
     *
     * <p>当前为日志告警占位实现，后续可接入飞书/钉钉/企微机器人或 Alertmanager。
     * 触发场景：外部依赖异常、系统异常、未知异常。</p>
     *
     * @param type    告警类型（EXTERNAL / SYSTEM / UNKNOWN）
     * @param code    错误码
     * @param message 错误消息
     * @param request HTTP 请求（用于提取请求方法与 URI）
     */
    private void triggerAlert(String type, int code, String message, HttpServletRequest request) {
        // TODO: 接入飞书/钉钉/企微机器人或 Alertmanager
        // 当前实现：ERROR 级别日志告警，由 ELK / 日志监控平台采集并触发告警规则
        log.error("[告警触发] type={}, code={}, msg={}, method={}, uri={}",
                type, code, message, request.getMethod(), request.getRequestURI());
    }

    private String buildErrorMessage(List<FieldErrorItem> errors) {
        if (errors == null || errors.isEmpty()) {
            return "参数校验失败";
        }
        return errors.stream()
                .map(e -> e.getField() + ": " + e.getMessage())
                .collect(Collectors.joining("; "));
    }

    private String extractField(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath().toString();
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    /**
     * 字段错误项
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class FieldErrorItem {
        private String field;
        private String message;
    }
}
