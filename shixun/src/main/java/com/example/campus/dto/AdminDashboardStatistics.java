package com.example.campus.dto;

import lombok.Data;

import java.math.BigDecimal;

//数量统计的封装
@Data
public class AdminDashboardStatistics {
    private Long userCount;
    private Long orderCount;
    private Long pendingVerifyCount;
    private Long pendingAppealCount;
    private Long todayOrderCount;
    private BigDecimal totalAmount;
}
