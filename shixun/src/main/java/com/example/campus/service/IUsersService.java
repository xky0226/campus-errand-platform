package com.example.campus.service;

import com.example.campus.dto.AdminLoginRequest;
import com.example.campus.dto.LoginResponse;
import com.example.campus.entity.Users;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author coder
 * @since 2026-09-20
 */
public interface IUsersService extends IService<Users> {

    //管理员登录：校验账号密码，通过则签发 token
    LoginResponse adminLogin(AdminLoginRequest request);

}
