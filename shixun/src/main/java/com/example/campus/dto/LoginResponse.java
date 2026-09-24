package com.example.campus.dto;


import com.example.campus.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Users user;
    //以下 6 个字段是讲义「4、移动端-构建」7.1 节新增的：
    //小程序端登录页的 saveAuthData() 直接读 res.token / res.userId / res.nickname ...
    //（注意前端拿到的是 data 载荷，不是整个 Result 信封）
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer role;
    private Integer verifiedStatus;
    private Integer creditScore;

    //管理员登录（讲义 3-2）只用 token + user 两个字段，这里保留原来的两参构造，
    //免得去改动上一份讲义已经跑通并验收过的 UsersServiceImpl.adminLogin()。
    public LoginResponse(String token, Users user) {
        this.token = token;
        this.user = user;
    }
}
