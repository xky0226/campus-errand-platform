package com.example.campus.common;

public class BusinessException extends RuntimeException {
    //定义编码
    private final Integer code;

    //初始化异常信息
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
