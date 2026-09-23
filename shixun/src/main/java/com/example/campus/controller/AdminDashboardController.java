package com.example.campus.controller;

import com.example.campus.dto.AdminDashboardStatistics;
import com.example.campus.dto.AdminOrderTrendItem;
import com.example.campus.dto.Result;
import com.example.campus.entity.Orders;
import com.example.campus.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/statistics")
    public Result<AdminDashboardStatistics> statistics() {
        return Result.success(adminDashboardService.getStatistics());
    }

    @GetMapping("/recent-orders")
    public Result<List<Orders>> recentOrders() {
        return Result.success(adminDashboardService.getRecentOrders());
    }

    @GetMapping("/order-trend")
    public Result<List<AdminOrderTrendItem>> orderTrend() {
        return Result.success(adminDashboardService.getOrderTrend());
    }

}
