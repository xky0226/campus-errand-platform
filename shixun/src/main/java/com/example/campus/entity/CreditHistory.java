package com.example.campus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 信用分变动历史记录表
 * </p>
 * <p>
 * 表在 campus.sql 里早就建好了（还带着一条种子数据：「订单完成: 5星 (发布者确认收货)」+3 分），
 * 但 Java 端一直没有实体，所以仲裁扣信用分只改了 users.credit_score，不留痕 ——
 * 用户看到分数掉了也无从查证。2026-10-15 补上，改分统一走 CreditScoreService。
 * <p>
 * 注意：这张表**没有 deleted 列**，千万别挂 {@code @TableLogic}，
 * 挂了之后所有查询都会自动拼 {@code AND deleted=0}，直接报 Unknown column。
 *
 * @author coder
 * @since 2026-10-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("credit_history")
public class CreditHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 变动值：正数加分，负数扣分。记的是**实际发生的**变动，见 CreditScoreServiceImpl */
    private Integer delta;

    private Integer beforeScore;

    private Integer afterScore;

    private String reason;

    /** 关联订单，与订单无关的调整（比如管理员手动调整）就是 null */
    private Long relatedOrderId;

    private LocalDateTime createdAt;
}
