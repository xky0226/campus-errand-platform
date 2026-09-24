package com.example.campus.service;

import com.example.campus.dto.AdminOrderRequest;
import com.example.campus.dto.OrderCreateRequest;
import com.example.campus.entity.Orders;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 订单表 服务类
 * </p>
 *
 * @author coder
 * @since 2026-09-21
 */
public interface IOrdersService extends IService<Orders> {

    //查询业务逻辑方法
    public List<Orders> listOrders();

    //根据id 查询订单详细
    public Orders getOrderDetail(Long orderId);

    //管理员新增订单，返回新增后的订单（带自增id）
    public Orders createOrder(AdminOrderRequest request);

    //管理员编辑订单，只允许编辑未开始执行的订单
    public Boolean updateOrder(Long orderId, AdminOrderRequest request);

    //强制取消订单，只允许取消未完成的订单
    public Boolean forceCancelOrder(Long orderId);

    //删除订单（逻辑删除）
    public Boolean deleteOrder(Long orderId);

    //========== 以下为讲义「4、移动端-构建」第 12.1 节新增的小程序端（客户端）接口 ==========

    //用户发布订单
    public Orders createClientOrder(Long publisherId, OrderCreateRequest request);

    //可接订单（status=1 待接单）
    public List<Orders> getAvailableOrders();

    //我发布的订单
    public List<Orders> getPublisherOrders(Long publisherId);

    //我接取的订单
    public List<Orders> getRunnerOrders(Long runnerId);

    //根据id查询订单（Controller 用的入口名，内部就是 getOrderDetail）
    public Orders getOrderById(Long orderId);

    //支付订单
    public Orders payOrder(Long userId, Long orderId, String payMethod);

    //接单
    public Orders acceptOrder(Long runnerId, Long orderId);

    //跑腿员确认送达
    public Orders confirmDelivery(Long runnerId, Long orderId);

    //发布者确认收货
    public Orders confirmReceipt(Long publisherId, Long orderId);

    //取消订单
    public Orders cancelOrder(Long userId, Long orderId, String reason);

    //用户编辑订单，只允许改未开始执行的订单
    public Orders updateOrder(Long userId, Long orderId, OrderCreateRequest request);

    //评价订单
    public void rateOrder(Long userId, Long orderId, Integer rating, String comment);
}
