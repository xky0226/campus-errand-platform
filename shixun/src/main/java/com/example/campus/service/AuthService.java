package com.example.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.campus.common.BusinessException;
import com.example.campus.common.JwtUtil;
import com.example.campus.dto.LoginResponse;
import com.example.campus.dto.WxLoginRequest;
import com.example.campus.entity.Users;
import com.example.campus.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 微信小程序登录。
 * 讲义「4、移动端-构建」7.3 节只列了这个类要负责的 5 件事，没给代码，
 * 这里按那 5 步实现，保持单一职责：AuthController 里的 /auth/wx-login 直接调它。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsersMapper userMapper;
    private final JwtUtil jwtUtil;

    //小程序 appid / secret 放在 application.yml，本机没配就为空，见 exchangeOpenId()
    @Value("${wx.miniapp.appid:}")
    private String appid;

    @Value("${wx.miniapp.secret:}")
    private String secret;

    //code2session 的接口地址
    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session"
                    + "?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    public LoginResponse wxLogin(WxLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getCode())) {
            throw new BusinessException("缺少微信登录 code");
        }

        //1. 用小程序 code 换取 openid
        String openId = exchangeOpenId(request.getCode());

        //2. 根据 openid 查询用户
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getOpenId, openId).eq(Users::getDeleted, 0);
        Users user = userMapper.selectOne(wrapper);

        //3. 用户不存在时自动创建用户
        if (user == null) {
            user = new Users();
            user.setOpenId(openId);
            user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : "微信用户");
            user.setAvatarUrl(request.getAvatarUrl());
            user.setStudentId("");
            user.setCreditScore(100);
            user.setRole(0);
            user.setStatus(1);
            user.setVerifiedStatus(0);
            userMapper.insert(user);
        }

        //4. 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getCreditScore());

        //5. 返回 LoginResponse
        return new LoginResponse(
                token,
                user,
                user.getId(),
                user.getRealName() != null && !user.getRealName().isEmpty() ? user.getRealName() : user.getNickname(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getVerifiedStatus(),
                user.getCreditScore()
        );
    }

    //拿 code 去微信换 openid；appid/secret 没配就直接报错，不返回假的 openid
    private String exchangeOpenId(String code) {
        if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret)) {
            throw new BusinessException("后端未配置微信小程序 appid/secret，无法使用微信一键登录");
        }
        String url = String.format(JSCODE2SESSION_URL, appid, secret, code);
        Map<?, ?> body;
        try {
            body = new RestTemplate().getForObject(url, Map.class);
        } catch (Exception e) {
            throw new BusinessException("调用微信接口失败：" + e.getMessage());
        }
        Object openId = body == null ? null : body.get("openid");
        if (openId == null) {
            Object errMsg = body == null ? null : body.get("errmsg");
            throw new BusinessException("微信登录失败：" + (errMsg != null ? errMsg : "未返回 openid"));
        }
        return openId.toString();
    }
}
