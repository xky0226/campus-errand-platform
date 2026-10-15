package com.example.campus.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值 / 提现的请求体。
 * 两个接口共用一个 DTO：充值时用 amount + payMethod，提现时用 amount + account。
 */
@Data
public class WalletAmountRequest {

    /** 金额，用 BigDecimal 接收，避免 double 的精度问题 */
    private BigDecimal amount;

    /** 充值方式，目前只是记进流水备注（模拟充值，没有真实支付渠道） */
    private String payMethod;

    /** 提现收款账号，比如支付宝账号 */
    private String account;
}
