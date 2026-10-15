package com.example.campus.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 钱包流水表
 * </p>
 *
 * @author coder
 * @since 2026-10-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("wallet_transactions")
public class WalletTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 交易号，对应 uk_tx_no 唯一索引 */
    private String transactionNo;

    /**
     * 金额。库里一律存正数，是收入还是支出由 type 决定，
     * 这样和已有的测试数据（order_pay、withdraw 也都是正数）保持一致。
     */
    private BigDecimal amount;

    /**
     * 交易类型：
     * recharge 充值 / withdraw 提现 / order_pay 订单支付 /
     * order_earn 订单收益 / order_refund 订单退款
     */
    private String type;

    private Long orderId;

    /** 0 处理中（比如提现申请还没审核），1 已完成 */
    private Integer status;

    private String remark;

    private String alipayTradeNo;

    private String thirdPartyAccount;

    /** DDL 里是 varchar(50)，所以用字符串存 */
    private String payTime;

    private Long relatedId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    /**
     * 收 / 支方向：1 收入，-1 支出。
     * 数据库里没有这一列，由 Service 按 type 算好给前端，省得小程序端再写一遍映射。
     */
    @TableField(exist = false)
    private Integer direction;

}
