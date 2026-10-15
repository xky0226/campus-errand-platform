package com.example.campus.controller;

import com.example.campus.common.BusinessException;
import com.example.campus.dto.AdminDashboardStatistics;
import com.example.campus.dto.AdminOrderTrendItem;
import com.example.campus.dto.Result;
import com.example.campus.entity.Orders;
import com.example.campus.service.AdminDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 - 首页看板。
 * <p>
 * 这三个接口原来只被 JWT 拦截器挡着「登录了没有」，没挡「是不是管理员」——
 * 任何登录用户（包括普通用户）都能拿到全平台的订单量和成交额。
 * 本次统一补上 requireAdmin，和 AdminController / AdminFinanceController 保持一致。
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/statistics")
    public Result<AdminDashboardStatistics> statistics(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(adminDashboardService.getStatistics());
    }

    @GetMapping("/recent-orders")
    public Result<List<Orders>> recentOrders(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(adminDashboardService.getRecentOrders());
    }

    @GetMapping("/order-trend")
    public Result<List<AdminOrderTrendItem>> orderTrend(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(adminDashboardService.getOrderTrend());
    }

    /** 管理端接口统一走这个：JWT 拦截器已经把 role 塞进 request 了 */
    private void requireAdmin(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        //管理员角色role为2、0为需求者、1为跑腿员
        if (role == null || role != 2) {
            throw new BusinessException("无管理员权限");
        }
    }

}
