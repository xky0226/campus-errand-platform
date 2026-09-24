package com.example.campus.dto;

import lombok.Data;

/**
 * 微信小程序一键登录的入参。
 * 讲义「4、移动端-构建」7.3 节只写了 wxLogin(WxLoginRequest request)，
 * 没有给这个类的字段，字段按小程序端 wxLogin(code, nickname, avatarUrl) 的调用反推。
 */
@Data
public class WxLoginRequest {
    private String code;
    private String nickname;
    private String avatarUrl;
}
