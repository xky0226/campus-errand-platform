package com.example.campus.dto;

import lombok.Data;

import java.util.List;

/**
 * 移动端提交申诉的入参（需求 3.5.3 纠纷仲裁）。
 *
 * 注意这里**没有** appealerId / defendantId：申诉人和被申诉人一律由后端拿订单反推，
 * 不信前端传的。campus.sql 里几条种子申诉的 appealer_id 跟订单的发布者/跑腿员对不上，
 * 说明这个字段历史上被写坏过，仲裁要动钱，不能把这个错误继续放大。
 */
@Data
public class AppealCreateRequest {

    /** 要申诉的订单 id */
    private Long orderId;

    /** 申诉理由，5~500 字 */
    private String reason;

    /** 证据图片地址（先调 POST /files/upload 拿到 /files/... 路径），最多 3 张 */
    private List<String> evidenceUrls;
}
