package com.example.campus.dto;

import lombok.Data;

/**
 * 管理端实名认证审核的请求体。
 * <p>
 * 路径对齐 permissions 表第 13 行：{@code admin:verify:pending} → {@code /admin/users/pending-verify}，
 * 审核动作本身走 {@code POST /admin/users/{id}/verify}。
 */
@Data
public class VerifyAuditRequest {

    /** true = 通过（verified_status → 1），false = 驳回（→ 0，并清掉学生证图让用户重传） */
    private Boolean pass;

    /**
     * 驳回理由。驳回时必填。
     * <p>
     * ⚠️ 目前**没有地方持久化它** —— users 表没有「驳回理由」列，
     * 本次范围也不动表结构。所以它只会写进服务端日志、并在响应里回给管理端，
     * 用户端看不到。这是已知缺口，不是忘了写。
     */
    private String reason;
}
