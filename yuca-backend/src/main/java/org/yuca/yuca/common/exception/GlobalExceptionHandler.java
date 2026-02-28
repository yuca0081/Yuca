package org.yuca.yuca.common.exception;

import org.yuca.yuca.common.response.Result;
import org.yuca.yuca.common.response.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 获取所有字段错误
        String errors = e.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> String.format("[%s: %s] (rejected value: %s)",
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()))
            .collect(Collectors.joining(", "));

        String simpleMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

        log.warn("参数校验失败: {}", errors);
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), simpleMessage);
    }

    /**
     * 参数绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> String.format("[%s: %s] (rejected value: %s)",
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()))
            .collect(Collectors.joining(", "));

        String simpleMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

        log.warn("参数绑定失败: {}", errors);
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), simpleMessage);
    }

    /**
     * 非法参数异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 非法状态异常处理
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleIllegalStateException(IllegalStateException e) {
        log.warn("非法状态: {}", e.getMessage(), e);
        return Result.error(ErrorCode.FORBIDDEN.getCode(), e.getMessage());
    }

    /**
     * 空指针异常处理
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "系统内部错误");
    }

    /**
     * 运行时异常处理
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("================== 异常堆栈开始 ==================", e);
        log.error("================== 异常堆栈结束 ==================");
        return Result.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), e.getMessage());
    }

    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("================== 异常堆栈开始 ==================", e);
        log.error("================== 异常堆栈结束 ==================");
        return Result.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "系统内部错误: " + e.getMessage());
    }
}
