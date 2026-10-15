package com.example.campus.dto;

import lombok.Data;

/**
 * 管理端手动调整信用分的请求体。
 * <p>
 * 路径对齐 permissions 表第 8 行：{@code admin:user:credit} → {@code /admin/users/*&#47;credit}。
 */
@Data
public class CreditAdjustRequest {

    /** 变动值：正数加分，负数扣分。不接受 0 */
    private Integer delta;

    /** 变动原因，会存进 credit_history.reason 给用户看，必填 */
    private String reason;
}
