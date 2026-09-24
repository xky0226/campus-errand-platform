package com.example.campus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderCreateRequest {
    private Long userId;
    private String taskType;
    private String pickupLocation;
    private String deliveryLocation;
    private String pickupLatitude;
    private String pickupLongitude;
    private String deliveryLatitude;
    private String deliveryLongitude;
    private String description;
    private String imageUrls;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expectedTime;

    private BigDecimal reward;
}
