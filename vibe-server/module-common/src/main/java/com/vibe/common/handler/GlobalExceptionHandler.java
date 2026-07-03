package com.vibe.common.handler;

import com.vibe.common.exception.BusinessException;
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
 * <p>统一捕获 Controller 层抛出的异常，按 {@link ResultCode} 规范返回统一响应体。</p>
 *
 * <ul>
 *   <li>{@link BusinessException}        → 返回对应业务错误码</li>
 *   <li>{@link MethodArgumentNotValidException} → 参数校验错误 400xx</li>
 *   <li>{@link BindException}            → 参数绑定错误 400xx</li>
 *   <li>{@link ConstraintViolationException} → 参数约束校验错误 400xx</li>
 *   <li>{@link AccessDeniedException}    → 权限不足 403xx</li>
 *   <li>{@link NoHandlerFoundException}  → 资源不存在 404xx</li>
 *   <li>{@link Exception}                → 系统错误 500xx</li>
 * </ul>
 *
 * @author vibe
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ============ 业务异常 ============ */

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("[业务异常] {} {} -> code={}, msg={}", request.getMethod(), request.getRequestURI(),
                ex.getCode(), ex.getMessage());
        return Result.<Void>fail(ex.getCode(), ex.getMessage()).withTimestamp(Instant.now().toEpochMilli());
    }

    /* ============ 参数校验异常：@Valid RequestBody ============ */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<FieldErrorItem>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                     HttpServletRequest request) {
        List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        String msg = buildErrorMessage(errors);
        log.warn("[参数校验失败] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        Result<List<FieldErrorItem>> result = Result.fail(ResultCode.PARAM_INVALID, msg);
        result.setData(errors);
        return result;
    }

    /* ============ 参数校验异常：表单绑定 ============ */

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<FieldErrorItem>> handleBindException(BindException ex, HttpServletRequest request) {
        List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        String msg = buildErrorMessage(errors);
        log.warn("[参数绑定失败] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        Result<List<FieldErrorItem>> result = Result.fail(ResultCode.PARAM_BIND_ERROR, msg);
        result.setData(errors);
        return result;
    }

    /* ============ 参数校验异常：@Validated 路径/查询参数 ============ */

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<List<FieldErrorItem>> handleConstraintViolation(ConstraintViolationException ex,
                                                                  HttpServletRequest request) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(cv -> new FieldErrorItem(extractField(cv), cv.getMessage()))
                .collect(Collectors.toList());
        String msg = buildErrorMessage(errors);
        log.warn("[约束校验失败] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        Result<List<FieldErrorItem>> result = Result.fail(ResultCode.PARAM_INVALID, msg);
        result.setData(errors);
        return result;
    }

    /* ============ 必填参数缺失 ============ */

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String msg = "缺少必填参数: " + ex.getParameterName();
        log.warn("[参数缺失] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        return Result.fail(ResultCode.PARAM_MISSING, msg);
    }

    /* ============ 参数类型不匹配 ============ */

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String msg = "参数 " + ex.getName() + " 类型不匹配";
        log.warn("[参数类型不匹配] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        return Result.fail(ResultCode.PARAM_TYPE_MISMATCH, msg);
    }

    /* ============ 请求体不可读/JSON 格式错误 ============ */

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("[请求体格式错误] {} {}", request.getMethod(), request.getRequestURI());
        return Result.fail(ResultCode.HTTP_MESSAGE_NOT_READABLE);
    }

    /* ============ 请求方法不支持 ============ */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String msg = "请求方法不支持: " + ex.getMethod();
        log.warn("[方法不支持] {} {} -> {}", request.getMethod(), request.getRequestURI(), msg);
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    /* ============ 权限不足 ============ */

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[权限不足] {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return Result.fail(ResultCode.FORBIDDEN);
    }

    /* ============ 资源不存在 ============ */

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("[资源不存在] {} {}", request.getMethod(), request.getRequestURI());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /* ============ 兜底：未知系统异常 ============ */

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("[系统异常] {} {}", request.getMethod(), request.getRequestURI(), ex);
        return Result.fail(ResultCode.INTERNAL_ERROR, "系统内部错误，请联系管理员");
    }

    /* ============ 辅助方法 ============ */

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
