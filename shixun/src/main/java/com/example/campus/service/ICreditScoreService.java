package com.example.campus.service;

import com.example.campus.entity.CreditHistory;

import java.util.List;

/**
 * <p>
 * 信用分 服务类
 * </p>
 * <p>
 * 存在的意义：改信用分这件事有两个副作用必须一起发生 —— 改 users.credit_score、
 * 往 credit_history 插一条记录。分两处写迟早会漏（仲裁那边就一直漏着）。
 * 所以把「改分+留痕」收口到这一个方法上，谁要改分都调它。
 *
 * @author coder
 * @since 2026-10-15
 */
public interface ICreditScoreService {

    /**
     * 增减信用分并留痕。
     *
     * @param userId         用户 ID
     * @param delta          变动值，正数加分、负数扣分
     * @param reason         变动原因，会原样存进 credit_history.reason，不能为空
     * @param relatedOrderId 关联订单，与订单无关的调整传 null
     * @return 新写入的历史记录；<b>没有实际变动时返回 null</b>
     * （delta 为 0，或者分数已经是 0 还继续扣），此时不写记录也不改分
     */
    CreditHistory adjust(Long userId, int delta, String reason, Long relatedOrderId);

    /** 某个用户的信用分变动历史，新的在前 */
    List<CreditHistory> history(Long userId);
}
