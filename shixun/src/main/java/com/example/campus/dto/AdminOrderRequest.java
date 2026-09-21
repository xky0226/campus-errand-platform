package com.example.campus.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminOrderRequest {
    private Long publisherId;

    @NotBlank(message = "订单类型不能为空")
    private String taskType;

    @NotNull(message = "悬赏金额不能为空")
    @DecimalMin(value = "1.00", message = "悬赏金额不能低于1元")
    private BigDecimal reward;

    @NotBlank(message = "取货地点不能为空")
    private String pickupLocation;

    private String pickupLatitude;
    private String pickupLongitude;

    @NotBlank(message = "送达地点不能为空")
    private String deliveryLocation;

    private String deliveryLatitude;
    private String deliveryLongitude;

    @NotNull(message = "预期完成时间不能为空")
    private LocalDateTime expectedTime;

    private String description;
    private BigDecimal deliveryDistance;
}
