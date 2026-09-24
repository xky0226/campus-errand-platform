package com.example.campus.controller;

import com.example.campus.dto.OrderCreateRequest;
import com.example.campus.dto.Result;
import com.example.campus.entity.Orders;
import com.example.campus.service.IOrdersService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 小程序端（普通用户）的订单接口。
 * 讲义「4、移动端-构建」第 10.3 节和第 12.2 节给了两个同名类，
 * 以第 12.2 节这个版本为准（用户身份取自 JWT 拦截器塞进 request 的 userId 属性，
 * 而不是从请求体里读 userId）。
 * 管理员那套 /admin/orders 仍在 OrdersController 里，两边互不影响。
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class ClientOrdersController {

    private final IOrdersService orderService;

    @PostMapping
    public Result<Orders> createOrder(@RequestBody OrderCreateRequest request,
                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(orderService.createClientOrder(userId, request));
    }

    @GetMapping("/available")
    public Result<List<Orders>> getAvailableOrders() {
        return Result.success(orderService.getAvailableOrders());
    }

    @GetMapping("/my/published")
    public Result<List<Orders>> getMyPublishedOrders(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(orderService.getPublisherOrders(userId));
    }

    @GetMapping("/my/running")
    public Result<List<Orders>> getMyRunningOrders(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(orderService.getRunnerOrders(userId));
    }

    @GetMapping("/{orderId}")
    public Result<Orders> getOrderDetail(@PathVariable Long orderId) {
        return Result.success(orderService.getOrderById(orderId));
    }

    @PostMapping("/{orderId}/pay")
    public Result<Orders> payOrder(@PathVariable Long orderId,
                                   @RequestBody(required = false) Map<String, String> body,
                                   HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String payMethod = body != null ? body.get("payMethod") : null;
        return Result.success(orderService.payOrder(userId, orderId, payMethod));
    }

    @GetMapping("/{orderId}/accept")
    public Result<Orders> acceptOrder(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(orderService.acceptOrder(userId, orderId));
    }

    @GetMapping("/{orderId}/deliver")
    public Result<Orders> confirmDelivery(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(orderService.confirmDelivery(userId, orderId));
    }

    @GetMapping("/{orderId}/confirm")
    public Result<Orders> confirmReceipt(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(orderService.confirmReceipt(userId, orderId));
    }

    @GetMapping("/{orderId}/cancel")
    public Result<Orders> cancelOrder(@PathVariable Long orderId,
                                      @RequestParam(required = false) String reason,
                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(orderService.cancelOrder(userId, orderId, reason != null ? reason : "用户主动取消"));
    }

    @PutMapping("/{orderId}")
    public Result<Orders> updateOrder(@PathVariable Long orderId,
                                      @RequestBody OrderCreateRequest request,
                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(orderService.updateOrder(userId, orderId, request));
    }

    @PostMapping("/{orderId}/rate")
    public Result<?> rateOrder(@PathVariable Long orderId,
                               @RequestBody Map<String, Object> body,
                               HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Number ratingValue = (Number) body.get("rating");
        Integer rating = ratingValue != null ? ratingValue.intValue() : null;
        String comment = (String) body.get("comment");
        orderService.rateOrder(userId, orderId, rating, comment);
        return Result.success("评价成功");
    }
}
