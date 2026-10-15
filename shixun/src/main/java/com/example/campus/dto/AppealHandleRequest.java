package com.example.campus.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端仲裁处理入参（需求 3.5.3）。
 *
 * 四个动作可以勾选组合，但 REJECT 与其它三个互斥 ——
 * 「驳回申诉」的含义是申诉不成立、订单恢复原状，再同时判一笔退款是自相矛盾的。
 */
@Data
public class AppealHandleRequest {

    /**
     * 仲裁动作，取值：
     * REFUND     强制退款 —— 把订单托管资金退回发布者，订单转已取消
     * COMPENSATE 补偿     —— 从责任方可用余额扣款赔给申诉方（罚款性质，不是平台垫付）
     * BAN        封号     —— 责任方账号停用
     * REJECT     驳回申诉 —— 订单恢复申诉前状态
     */
    private List<String> actions;

    /** COMPENSATE 专用：补偿金额，必须大于 0 */
    private BigDecimal amount;

    /** 管理员备注，会拼进 appeals.admin_remark */
    private String remark;
}
