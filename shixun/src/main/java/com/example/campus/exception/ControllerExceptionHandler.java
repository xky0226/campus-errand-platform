package com.example.campus.exception;

import com.example.campus.dto.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>
 * 全局异常处理器
 * </p>
 *
 * @author coder
 * @since 2026-09-21
 */
@RestControllerAdvice
public class ControllerExceptionHandler {

    //处理@Valid参数校验失败，只把字段上写的中文提示返回给前端
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidException(MethodArgumentNotValidException e){
        //一个请求可能同时有多条校验不通过，这里只取第一条
        return Result.error(e.getBindingResult().getFieldError().getDefaultMessage());
    }

    //处理所有未捕获的异常，统一转成Result以json形式返回
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e){
        return Result.error(e.getMessage());
    }

}
