package com.example.campus.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.campus.common.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // ========== DEBUG: 所有请求都打印 ==========
        log.info("========== JWT 拦截器开始 ==========");
        log.info("请求路径: method={}, path={}", method, path);

        // 如果是 /auth/profile，强制打印更详细的信息
        if (path.contains("/auth/profile")) {
            log.warn("===============> 检测到 /auth/profile 请求!");
        }

        // 跳过 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.info("跳过 OPTIONS 请求");
            return true;
        }

        // 白名单：不需要 token 的公开接口
        String[] whiteList = {
                "/auth/login",
                "/auth/wx-login",
                "/auth/admin-login",
                "/auth/register"
        };

        // 检查是否在白名单中（包含 /api 前缀也能匹配）
        for (String whitePath : whiteList) {
            if (path.endsWith(whitePath)) {
                log.info("白名单接口，跳过验证: {}", path);
                return true;
            }
        }
        // 用户公开信息接口也跳过
        if (path.contains("/auth/users/")) {
            log.info("用户公开信息接口，跳过验证: {}", path);
            return true;
        }

        // 支付状态查询和支付宝异步回调跳过验证，订单H5支付创建需要解析token获取用户身份
        if (path.contains("/api/payment/") || path.endsWith("/payment/alipay/notify")) {
            log.info("支付公开接口，跳过验证: {}", path);
            return true;
        }

        // 静态资源文件（图片等）跳过验证
        if (path.contains("/files/") || path.contains("/uploads/") || path.contains("/avatars/")) {
            log.info("静态资源文件，跳过验证: {}", path);
            return true;
        }

        log.info("需要验证 token，开始解析... path={}", path);

        // 1. 优先从请求头获取 token
        String token = request.getHeader("Authorization");
        log.info("请求头 Authorization: {}", token);

        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            log.info("提取后的 token: {}...", token.substring(0, Math.min(20, token.length())));
        } else {
            // 2. 从 URL 参数获取 token（用于 web-view 跳转）
            token = request.getParameter("token");
            if (StringUtils.hasText(token)) {
                log.info("从 URL 参数获取到 token: {}...", token.substring(0, Math.min(20, token.length())));
            } else {
                log.warn("没有找到有效的 token");
                writeUnauthorized(response, "请先登录");
                return false;
            }
        }

        if (!jwtUtil.validateToken(token)) {
            log.warn("token 验证失败或已过期");
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        Integer role = jwtUtil.getRole(token);
        log.info("token 解析成功 -> userId={}, role={}", userId, role);

        request.setAttribute("userId", userId);
        request.setAttribute("role", role);
        log.info("========== JWT 拦截器通过 ==========");
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
