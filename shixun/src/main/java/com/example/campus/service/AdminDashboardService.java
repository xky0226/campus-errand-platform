package com.example.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.campus.dto.AdminDashboardStatistics;
import com.example.campus.dto.AdminOrderTrendItem;
import com.example.campus.entity.Appeals;
import com.example.campus.entity.Orders;
import com.example.campus.entity.Users;
import com.example.campus.mapper.AppealsMapper;
import com.example.campus.mapper.OrdersMapper;
import com.example.campus.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final UsersMapper userMapper;
    private final OrdersMapper orderMapper;
    private final AppealsMapper appealMapper;

    public AdminDashboardStatistics getStatistics() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(today, LocalTime.MAX);

        AdminDashboardStatistics statistics = new AdminDashboardStatistics();
        statistics.setUserCount(userMapper.selectCount(new LambdaQueryWrapper<Users>()));
        statistics.setOrderCount(orderMapper.selectCount(new LambdaQueryWrapper<Orders>()));
        statistics.setPendingVerifyCount(userMapper.selectCount(
                new LambdaQueryWrapper<Users>().eq(Users::getVerifiedStatus, 2)
        ));
        statistics.setPendingAppealCount(appealMapper.selectCount(
                new LambdaQueryWrapper<Appeals>().eq(Appeals::getStatus, 0)
        ));
        statistics.setTodayOrderCount(orderMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .between(Orders::getCreatedAt, todayStart, todayEnd)
        ));
        statistics.setTotalAmount(orderMapper.sumPaidOrderAmount());
        return statistics;
    }

    public List<Orders> getRecentOrders() {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .orderByDesc(Orders::getCreatedAt)
                        .last("LIMIT 10")
        );
    }

    public List<AdminOrderTrendItem> getOrderTrend() {
        LocalDate startDate = LocalDate.now().minusDays(6);
        List<AdminOrderTrendItem> dbItems = orderMapper.countOrdersByDate(startDate);
        List<AdminOrderTrendItem> result = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateText = date.toString();
            AdminOrderTrendItem item = new AdminOrderTrendItem();
            item.setDate(dateText);
            item.setCount(dbItems.stream()
                    .filter(dbItem -> dateText.equals(dbItem.getDate()))
                    .map(AdminOrderTrendItem::getCount)
                    .findFirst()
                    .orElse(0));
            result.add(item);
        }
        return result;
    }
}
