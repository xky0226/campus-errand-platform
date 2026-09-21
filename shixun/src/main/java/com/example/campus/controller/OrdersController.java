package com.example.campus.controller;


import com.example.campus.dto.AdminOrderRequest;
import com.example.campus.dto.Result;
import com.example.campus.entity.Orders;
import com.example.campus.service.IOrdersService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

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

    //查询所有订单【没有删除的订单】
    @GetMapping
    public Result<List<Orders>> listOrders(){
        //使用Result类进行结果的封装，以json形式返回
        return Result.success(ordersService.listOrders());
    }

    //查询订单详情【orderId】
    @GetMapping("/{orderId}")
    public Result<Orders> getOrderDetail(@PathVariable Long orderId){
        return Result.success(ordersService.getOrderDetail(orderId));
    }

    //管理员新增订单  POST /admin/orders/create
    @PostMapping("/create")
    public Result<Orders> createOrder(@Valid @RequestBody AdminOrderRequest request){
        //使用Result类进行结果的封装，以json形式返回
        return Result.success(ordersService.createOrder(request));
    }

    //管理员编辑订单  POST /admin/orders/{orderId}/update
    @PostMapping("/{orderId}/update")
    public Result<Boolean> updateOrder(@PathVariable Long orderId,
                                       @Valid @RequestBody AdminOrderRequest request){
        return Result.success(ordersService.updateOrder(orderId, request));
    }

    //强制取消订单  POST /admin/orders/{orderId}/force-cancel
    @PostMapping("/{orderId}/force-cancel")
    public Result<Boolean> forceCancelOrder(@PathVariable Long orderId){
        return Result.success(ordersService.forceCancelOrder(orderId));
    }

    //删除订单【逻辑删除】  DELETE /admin/orders/{orderId}
    @DeleteMapping("/{orderId}")
    public Result<Boolean> deleteOrder(@PathVariable Long orderId){
        return Result.success(ordersService.deleteOrder(orderId));
    }

}
