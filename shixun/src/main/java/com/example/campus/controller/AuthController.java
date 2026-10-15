package com.example.campus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.campus.common.JwtUtil;
import com.example.campus.common.VerifyStatus;
import com.example.campus.dto.AdminLoginRequest;
import com.example.campus.dto.LoginResponse;
import com.example.campus.dto.Result;
import com.example.campus.dto.VerifySubmitRequest;
import com.example.campus.dto.WxLoginRequest;
import com.example.campus.entity.Users;
import com.example.campus.mapper.UsersMapper;
import com.example.campus.service.AuthService;
import com.example.campus.service.IUsersService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
//构造器配置注解
@RequiredArgsConstructor
public class AuthController {

    private final IUsersService usersService;
    //讲义「4、移动端-构建」7.1 / 7.2 节把小程序端的登录、注册逻辑直接写在 Controller 里，
    //所以这三个依赖也放在这里
    private final UsersMapper userMapper;
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    //加密算法（声明处就 new 了，不会被 @RequiredArgsConstructor 算进构造参数）
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    //   提供登录请求处理方法
    @PostMapping("/admin-login")
    public Result<LoginResponse> adminLogin(@Validated @RequestBody AdminLoginRequest request){
        //调用登录逻辑处理方法
        LoginResponse response = usersService.adminLogin(request);
        return Result.success(response);
    }

    //小程序端：学号 / 账号 + 密码登录
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return Result.error("请填写用户名和密码");
        }

        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getStudentId, username).eq(Users::getDeleted, 0);
        Users user = userMapper.selectOne(wrapper);

        if (user == null) {
            return Result.error("用户不存在，请检查学号");
        }
        if (user.getStatus() != 1) {
            return Result.error("账号已被冻结，请联系管理员");
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return Result.error("密码错误");
            }
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getCreditScore());
        return Result.success(new LoginResponse(
                token,
                user,
                user.getId(),
                user.getRealName() != null ? user.getRealName() : user.getNickname(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getVerifiedStatus(),
                user.getCreditScore()
        ));
    }

    //小程序端：用户注册
    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody Map<String, String> request) {
        String nickname = request.get("nickname");
        String username = request.get("username");
        String password = request.get("password");
        String phone = request.get("phone");

        if (nickname == null || nickname.isEmpty()) return Result.error("请输入昵称");
        if (username == null || username.isEmpty()) return Result.error("请输入学号");
        if (password == null || password.isEmpty()) return Result.error("请输入密码");
        if (phone == null || phone.isEmpty()) return Result.error("请输入手机号");

        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getStudentId, username).eq(Users::getDeleted, 0);
        long count = userMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("该学号已注册");
        }

        Users user = new Users();
        user.setNickname(nickname);
        user.setStudentId(username);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setOpenId("user_" + username + "_" + System.currentTimeMillis());
        user.setCreditScore(100);
        user.setRole(0);
        user.setStatus(1);
        user.setVerifiedStatus(0);
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getCreditScore());
        return Result.success(new LoginResponse(
                token,
                user,
                user.getId(),
                user.getRealName() != null ? user.getRealName() : user.getNickname(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getVerifiedStatus(),
                user.getCreditScore()
        ));
    }

    //小程序端：微信一键登录
    @PostMapping("/wx-login")
    public Result<LoginResponse> wxLogin(@RequestBody WxLoginRequest request) {
        return Result.success(authService.wxLogin(request));
    }

    // ================= 以下三个接口是「考核作业 - 个人中心」要用的 =================
    // 讲义「4、移动端-构建」§2 的接口总览表里列了 GET /api/auth/profile、
    // PUT /api/auth/profile、POST /api/auth/switch-role 这三个（前端 services/auth.js
    // 的 getProfile/updateProfile/switchRole 也是照 §8 抄的），但全篇没给后端实现代码。
    // 这里按 §2 表给出的契约补齐：路径、方法、前端怎么调，都跟表里一致。
    // 身份统一从 JWT 拦截器塞进 request 的 userId 属性取，不信任请求体里传来的 id。

    //获取当前登录用户的个人信息
    @GetMapping("/profile")
    public Result<Users> getProfile(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        //Users 没挂 @TableLogic（讲义 3-2 就这么建的），所以要自己判断 deleted
        Users user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == null || user.getDeleted() != 0) {
            return Result.error("用户不存在");
        }
        //password 字段上有 @JsonIgnore，不会吐出去
        return Result.success(user);
    }

    //更新个人信息（只允许改昵称、头像、手机号；学号 / 角色 / 信用分不能在这里改）
    @PutMapping("/profile")
    public Result<Users> updateProfile(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        String nickname = request.get("nickname");
        String avatarUrl = request.get("avatarUrl");
        String phone = request.get("phone");

        //Users 实体里 nickname 是 NOT NULL，空值不能写进去
        if (nickname != null && nickname.trim().isEmpty()) {
            return Result.error("昵称不能为空");
        }
        if (nickname != null && nickname.trim().length() > 64) {
            return Result.error("昵称不能超过64个字符");
        }
        if (avatarUrl != null && avatarUrl.length() > 512) {
            return Result.error("头像地址过长");
        }
        if (phone != null && phone.trim().length() > 16) {
            return Result.error("手机号格式不正确");
        }

        Users update = new Users();
        update.setId(userId);
        //MyBatis-Plus 的 updateById 默认只更新非 null 字段，
        //所以下面不 set 的字段不会被动到
        if (nickname != null) {
            update.setNickname(nickname.trim());
        }
        if (avatarUrl != null) {
            update.setAvatarUrl(avatarUrl.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            update.setPhone(phone.trim());
        }

        userMapper.updateById(update);
        return Result.success(userMapper.selectById(userId));
    }

    //切换角色（0 = 普通用户，1 = 跑腿员）
    @PostMapping("/switch-role")
    public Result<LoginResponse> switchRole(@RequestParam(required = false) Integer role,
                                            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        //只放开 0/1，role=2 是管理员，不能自己切
        if (role == null || (role != 0 && role != 1)) {
            return Result.error("角色只能是 0（普通用户）或 1（跑腿员）");
        }

        Users user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == null || user.getDeleted() != 0) {
            return Result.error("用户不存在");
        }
        if (user.getRole() != null && user.getRole() == 2) {
            return Result.error("管理员账号不能切换角色");
        }
        if (role.equals(user.getRole())) {
            return Result.error(role == 1 ? "当前已经是跑腿员" : "当前已经是普通用户");
        }

        Users update = new Users();
        update.setId(userId);
        update.setRole(role);
        userMapper.updateById(update);

        //重新签发 token：JWT 里的 role 是签发时固化进去的，
        //不换 token 的话拦截器塞给 Controller 的 role 还是旧的。
        //返回值故意做成和登录完全一样的形状，前端可以直接复用登录页那套存档逻辑
        Users fresh = userMapper.selectById(userId);
        String token = jwtUtil.generateToken(fresh.getId(), fresh.getRole(), fresh.getCreditScore());
        return Result.success(new LoginResponse(
                token,
                fresh,
                fresh.getId(),
                fresh.getRealName() != null ? fresh.getRealName() : fresh.getNickname(),
                fresh.getAvatarUrl(),
                fresh.getRole(),
                fresh.getVerifiedStatus(),
                fresh.getCreditScore()
        ));
    }

    // ================= 实名认证（需求 3.5.1 用户管理 / 移动端） =================

    /**
     * 提交实名认证资料，提交后进入「审核中」，等管理端在 /verify 页处理。
     * <p>
     * 为什么必须单开这个接口：{@code PUT /auth/profile} 明确只允许改昵称 / 头像 / 手机号，
     * 姓名、学号、学生证图用户自己根本提交不了 —— 没有这个接口，整条实名认证链路就没有起点，
     * 管理端的「认证审核」页永远是空的。
     */
    @PostMapping("/verify")
    public Result<Users> submitVerify(@RequestBody VerifySubmitRequest req, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        Users user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == null || user.getDeleted() != 0) {
            return Result.error("用户不存在");
        }
        // 已经认证过 / 正在审核中的不给重复提交，避免刷审核队列
        if (VerifyStatus.PASSED == intOr(user.getVerifiedStatus(), VerifyStatus.NONE)) {
            return Result.error("你已经通过实名认证，无需重复提交");
        }
        if (VerifyStatus.PENDING == intOr(user.getVerifiedStatus(), VerifyStatus.NONE)) {
            return Result.error("认证资料正在审核中，请耐心等待审核结果");
        }

        String realName = req.getRealName() == null ? "" : req.getRealName().trim();
        String studentId = req.getStudentId() == null ? "" : req.getStudentId().trim();
        String certUrl = req.getStudentCertUrl() == null ? "" : req.getStudentCertUrl().trim();

        // 姓名：2~10 个汉字。库里 seed 数据是「王明」「李同学」这种，不排除「XX同学」
        if (!realName.matches("^[\\u4e00-\\u9fa5]{2,10}$")) {
            return Result.error("请填写 2~10 个汉字的真实姓名");
        }
        if (studentId.isEmpty()) {
            return Result.error("请填写学号");
        }
        if (certUrl.isEmpty()) {
            return Result.error("请先上传学生证照片");
        }
        if (certUrl.length() > 512) {
            // users.student_cert_url 是 varchar(512)，超了会直接 Data too long
            return Result.error("学生证图片地址过长");
        }
        // 学号就是小程序端的登录账号（login 按 student_id 查用户）。
        // 这里只校验一致性、不改它：改了用户自己就登不进来，而且等于允许把账号过户给别人。
        if (!studentId.equals(user.getStudentId())) {
            return Result.error("学号与注册时填写的不一致（注册学号：" + user.getStudentId()
                    + "），如需修改请联系管理员");
        }

        Users patch = new Users();
        patch.setId(userId);
        patch.setRealName(realName);
        patch.setStudentCertUrl(certUrl);
        patch.setVerifiedStatus(VerifyStatus.PENDING);
        userMapper.updateById(patch);

        return Result.success(userMapper.selectById(userId));
    }

    private int intOr(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

}
