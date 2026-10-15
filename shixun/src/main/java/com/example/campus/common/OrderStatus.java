package com.example.campus.common;

/**
 * 订单状态码。取值以建表语句（campus.sql 的 orders.status 注释）为准。
 * <p>
 * 这些数字在 {@code OrdersServiceImpl} 的一堆 if 判空里散着用，导出报表时又需要中文名，
 * 所以收口到这里。注意 {@code AppealVO} 里还有一份私有的写法（那边是申诉详情页自己用的），
 * 新代码请走这个类。
 */
public final class OrderStatus {

    /** 待支付 */
    public static final int UNPAID = 0;
    /** 待接单 */
    public static final int WAITING = 1;
    /** 进行中 */
    public static final int RUNNING = 2;
    /** 待确认（跑腿员已送达，等发布者确认收货） */
    public static final int DELIVERED = 3;
    /** 已完成 */
    public static final int FINISHED = 4;
    /** 已取消 */
    public static final int CANCELLED = 5;
    /** 申诉中（提交申诉时从原状态转过来，驳回后还原） */
    public static final int APPEALING = 6;

    private OrderStatus() {
    }

    /**
     * 状态码 → 中文名。
     * 认不出来的取值返回「状态N」而不是硬套一个词 —— 编一个不存在的状态名比显示数字更误导人。
     */
    public static String name(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case UNPAID -> "待支付";
            case WAITING -> "待接单";
            case RUNNING -> "进行中";
            case DELIVERED -> "待确认";
            case FINISHED -> "已完成";
            case CANCELLED -> "已取消";
            case APPEALING -> "申诉中";
            default -> "状态" + status;
        };
    }
}
