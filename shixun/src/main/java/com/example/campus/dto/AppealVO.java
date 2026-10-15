package com.example.campus.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 申诉的展示对象（管理端仲裁中心 + 移动端「我的申诉」共用）。
 *
 * 为什么不直接返回 Appeals 实体：申诉列表光有 order_id / appealer_id 这些数字没法看，
 * 管理端要显示订单号、任务类型、金额和双方昵称。做成 VO 一次带出去，
 * 省得前端拿着 id 再一个个查订单详情（也就避免了列表页的 N+1 请求）。
 */
@Data
public class AppealVO {

    private Long id;

    //========== 关联订单 ==========
    private Long orderId;
    private String orderNo;
    private String taskType;
    /** orders.status：0待支付 1待接单 2进行中 3待确认 4已完成 5已取消 6申诉中 */
    private Integer orderStatus;
    private String orderStatusName;
    private BigDecimal reward;

    //========== 申诉双方 ==========
    private Long appealerId;
    private String appealerName;
    private Long defendantId;
    private String defendantName;

    //========== 申诉内容 ==========
    private String reason;
    /** 证据图地址列表，已从 evidence_urls 的逗号串拆好并去掉空项 */
    private List<String> evidenceUrls;

    //========== 处理结果 ==========
    /** 0 待处理 / 1 已处理 */
    private Integer status;
    private String statusName;
    private String adminRemark;
    private Long adminId;
    private String adminName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 订单状态的中文名，管理端表格直接显示 */
    public static String orderStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "待接单";
            case 2 -> "进行中";
            case 3 -> "待确认";
            case 4 -> "已完成";
            case 5 -> "已取消";
            case 6 -> "申诉中";
            default -> "未知(" + status + ")";
        };
    }
}
