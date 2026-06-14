package com.blog.exception;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 资源不存在 */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("资源不存在: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验失败（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(ResultCode.VALID_ERROR.getCode(), msg);
    }

    /** 参数校验失败（@Validated + 单个参数） */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Result.error(ResultCode.VALID_ERROR.getCode(), msg);
    }

    /** 请求参数缺失 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.badRequest("缺少必填参数: " + e.getParameterName());
    }

    /** 参数类型错误 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.badRequest("参数类型错误: " + e.getName());
    }

    /** 事务异常 */
    @ExceptionHandler(TransactionSystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleTransactionSystemException(TransactionSystemException e) {
        log.error("事务异常", e);
        return Result.error(500, "服务器内部错误");
    }

    /** 请求体解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.badRequest("请求体格式错误");
    }

    /** 参数非法 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.badRequest(e.getMessage());
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("未捕获异常: {} - {}", e.getClass().getName(), e.getMessage(), e);
        return Result.error(ResultCode.INTERNAL_ERROR);
    }
}
