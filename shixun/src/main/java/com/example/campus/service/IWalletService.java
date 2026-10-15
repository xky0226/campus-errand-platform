package com.example.campus.service;

import com.example.campus.entity.WalletTransaction;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 钱包 服务类
 * </p>
 *
 * @author coder
 * @since 2026-10-15
 */
public interface IWalletService extends IService<WalletTransaction> {

    //========== 给小程序端页面用的四个接口 ==========

    /** 查询余额：可用余额、冻结金额（已支付但订单未完成的托管资金）、累计收益 */
    Map<String, Object> getBalance(Long userId);

    /** 查询流水，type 为空表示查全部 */
    List<WalletTransaction> listTransactions(Long userId, String type, Integer limit);

    /** 模拟充值：直接给余额加上金额并记一条流水 */
    WalletTransaction recharge(Long userId, BigDecimal amount, String payMethod);

    /** 申请提现：余额转入冻结，生成一条待审核流水，审核通过后才真正扣减 */
    WalletTransaction withdraw(Long userId, BigDecimal amount, String account);

    //========== 给订单模块调用（B2 订单支付打通钱包） ==========

    /** 支付订单：可用余额 → 冻结余额（平台托管），订单完成后才结算给跑腿员 */
    void payForOrder(Long publisherId, Long orderId, BigDecimal amount);

    /** 订单完成结算：把托管资金扣出，打给跑腿员 */
    void settleToRunner(Long publisherId, Long runnerId, Long orderId, BigDecimal amount);

    /** 订单取消 / 仲裁退款：把托管资金退回发布者余额 */
    void refundToPublisher(Long publisherId, Long orderId, BigDecimal amount, String remark);

    //========== 给申诉仲裁调用（3.5.3 纠纷仲裁） ==========

    /**
     * 这笔订单现在还剩多少可支配的托管资金。
     *
     * 为什么要单独开一个查询：refundToPublisher / settleToRunner 开头都有
     * hasFrozenForOrder 判断，查不到 order_pay 流水就 log.warn 之后直接 return ——
     * 不报错也不退钱。campus.sql 里的历史订单（钱包打通之前那批）恰好全都没有这条流水，
     * 仲裁要是直接调退款，就会出现「提示仲裁成功、实际一分钱没退」。
     * 所以仲裁在动钱之前先问一句还剩下多少，不足就明确报错，绝不假装成功。
     */
    BigDecimal escrowRemaining(Long orderId);

    /**
     * 仲裁补偿（罚款性质）：从责任方可用余额扣款，加到申诉方可用余额。
     *
     * 口径说明：平台没有自己的账户（users 里没有平台账号，唯一的管理员余额也是 0），
     * 所以「补偿」不是平台垫付，而是从被判有责任的一方扣钱赔给受损方 ——
     * 这也和 campus.sql 里种子的 admin_remark「核实后确实送错，已罚款跑腿员」一致。
     *
     * @param fromUserId 被判有责任的一方（扣款方）
     * @param toUserId   受损方（收款方）
     */
    WalletTransaction compensate(Long fromUserId, Long toUserId, Long orderId,
                                 BigDecimal amount, String remark);

    //========== 给管理端财务模块调用 ==========

    /** 查询全部提现申请（管理端审核用），status 为空表示查全部 */
    List<WalletTransaction> listWithdrawals(Integer status);

    /** 审核提现：pass=true 通过（扣掉冻结），pass=false 驳回（退回可用余额） */
    WalletTransaction auditWithdraw(Long adminId, Long txId, boolean pass, String remark);

    /** 平台收入统计用的流水汇总，返回 { order_pay, order_earn, recharge, withdraw } 各自的笔数与金额 */
    Map<String, Object> statistics();

    /**
     * 营收趋势（需求 3.5.4）。按 period 分桶统计 order_pay 的交易额，
     * period 取 day（最近 7 天）/ week（最近 6 周）/ month（最近 6 个月），不传按 day。
     * 没有交易的时间桶也会补一条 0，前端画折线不用再自己补空洞。
     */
    List<Map<String, Object>> revenueTrend(String period);

    /** 导出财务报表用的流水明细（新的在前），每行把中文类型名、收支方向、用户昵称都拼好 */
    List<Map<String, Object>> transactionReport();
}
