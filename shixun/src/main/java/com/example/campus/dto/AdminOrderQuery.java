package com.example.campus.dto;

import lombok.Data;

//订单列表查询参数 DTO
@Data
public class AdminOrderQuery {
    private Integer status;
    private String keyword;
    private Integer page = 1;
    private Integer pageSize = 10;
}
