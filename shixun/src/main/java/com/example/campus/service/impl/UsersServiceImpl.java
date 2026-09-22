package com.example.campus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.campus.common.BusinessException;
import com.example.campus.common.JwtUtil;
import com.example.campus.dto.AdminLoginRequest;
import com.example.campus.dto.LoginResponse;
import com.example.campus.entity.Users;
import com.example.campus.mapper.UsersMapper;
import com.example.campus.service.IUsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author coder
 * @since 2026-09-20
 */
@Service
@RequiredArgsConstructor
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements IUsersService {

    private final UsersMapper userMapper;
    //token工具类
    private final JwtUtil jwtUtil;
    //加密算法（在声明处就 new 了，所以不会被 @RequiredArgsConstructor 算进构造参数）
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginResponse adminLogin(AdminLoginRequest request) {
        //条件构造器【根据RealName】
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getRealName, request.getUsername());
        //条件查询
        Users user = userMapper.selectOne(wrapper);
        //user是否有记录
        //注意：Users 的 deleted 字段没有挂 @TableLogic，这里也没过滤 deleted，
        //所以已注销（deleted=1）但 status 不为 0 的账号仍然能登录 —— 讲义原文就是如此
        if(user==null){
            throw new BusinessException("账号或密码错误");
        }
        //密文校验
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException("账号或密码错误");
            }
        }
        //校验用户状态，通过则生成token
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        //返回token和user对象数据
        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getCreditScore());
        return new LoginResponse(token, user);
    }
}
