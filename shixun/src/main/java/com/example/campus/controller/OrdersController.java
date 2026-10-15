package com.example.campus.controller;


import com.example.campus.common.BusinessException;
import com.example.campus.common.CsvExporter;
import com.example.campus.common.OrderStatus;
import com.example.campus.dto.AdminOrderQuery;
import com.example.campus.dto.AdminOrderRequest;
import com.example.campus.dto.Result;
import com.example.campus.entity.Orders;
import com.example.campus.service.IOrdersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 订单表 前端控制器
 * </p>
 *
 * @author coder
 * @since 2026-09-21
 */
@RestController
@RequestMapping("/admin/orders")
public class OrdersController {

    //获取业务层对象
    @Autowired
    IOrdersService ordersService;

    /**
     * 查询订单列表，支持按状态 + 关键词筛选（需求 3.5.2）。
     * <p>
     * 不传参数时行为跟以前完全一样（返回全部未删除订单），所以 {@code Orders.vue}
     * 和 tests/ 里的用例都不用改。筛选条件全部由数据库执行，前端不再自己 filter。
     */
    @GetMapping
    public Result<List<Orders>> listOrders(AdminOrderQuery query, HttpServletRequest request) {
        requireAdmin(request);
        //使用Result类进行结果的封装，以json形式返回
        return Result.success(ordersService.listOrders(query));
    }

    /**
     * 导出订单报表（需求 3.5.2），直接返回 CSV 字节流给浏览器下载。
     * <p>
     * 筛选用和列表页同一套条件，所以「先筛出想要的那批再导出」是自然的用法。
     * 注意返回的是 {@code ResponseEntity<byte[]>} 而不是 {@code Result} ——
     * 这个接口是给人下载文件的，不是给前端解 JSON 的。
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrders(AdminOrderQuery query, HttpServletRequest request) {
        requireAdmin(request);

        List<Orders> orders = ordersService.listOrders(query);

        List<List<Object>> rows = new ArrayList<>(orders.size());
        for (Orders order : orders) {
            List<Object> row = new ArrayList<>(10);
            row.add(order.getOrderNo());
            row.add(order.getTaskType());
            // 金额交给 Excel 当数字处理，所以不加「¥」（加了就成文本了，没法求和）
            row.add(order.getReward());
            row.add(OrderStatus.name(order.getStatus()));
            row.add(order.getPublisherName());
            row.add(order.getRunnerName());
            row.add(order.getPickupLocation());
            row.add(order.getDeliveryLocation());
            row.add(order.getDescription());
            row.add(order.getCreatedAt());
            rows.add(row);
        }

        List<String> headers = List.of("订单号", "任务类型", "报酬(元)", "状态", "发布者", "跑腿员",
                "取货地点", "送达地点", "任务描述", "创建时间");

        String filename = "订单报表" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
        return CsvExporter.download(filename, CsvExporter.toCsvBytes(headers, rows));
    }

    //查询订单详情【orderId】
    @GetMapping("/{orderId}")
    public Result<Orders> getOrderDetail(@PathVariable Long orderId, HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(ordersService.getOrderDetail(orderId));
    }

    //管理员新增订单  POST /admin/orders/create
    @PostMapping("/create")
    public Result<Orders> createOrder(@Valid @RequestBody AdminOrderRequest request, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        //使用Result类进行结果的封装，以json形式返回
        return Result.success(ordersService.createOrder(request));
    }

    //管理员编辑订单  POST /admin/orders/{orderId}/update
    @PostMapping("/{orderId}/update")
    public Result<Boolean> updateOrder(@PathVariable Long orderId,
                                       @Valid @RequestBody AdminOrderRequest request,
                                       HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return Result.success(ordersService.updateOrder(orderId, request));
    }

    //强制取消订单  POST /admin/orders/{orderId}/force-cancel
    @PostMapping("/{orderId}/force-cancel")
    public Result<Boolean> forceCancelOrder(@PathVariable Long orderId, HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(ordersService.forceCancelOrder(orderId));
    }

    //删除订单【逻辑删除】  DELETE /admin/orders/{orderId}
    @DeleteMapping("/{orderId}")
    public Result<Boolean> deleteOrder(@PathVariable Long orderId, HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(ordersService.deleteOrder(orderId));
    }

    /**
     * 管理端接口统一走这个：JWT 拦截器已经把 role 塞进 request 了。
     * <p>
     * 这个类是本次补的：整组 {@code /admin/orders} 之前只有 JWT 拦截器挡着「登录了没有」，
     * 没挡「是不是管理员」，普通用户拿自己的 token 就能直接调管理员的订单管理接口
     * （包括删除订单）。其它管理端 Controller（AdminController / AdminFinanceController /
     * AdminAppealController）都有这个校验，这里跟着补齐。
     */
    private void requireAdmin(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        //管理员角色role为2、0为需求者、1为跑腿员
        if (role == null || role != 2) {
            throw new BusinessException("无管理员权限");
        }
    }

}
