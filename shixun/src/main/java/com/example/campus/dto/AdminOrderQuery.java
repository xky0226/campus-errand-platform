package com.example.campus.dto;

import lombok.Data;

/**
 * 订单列表查询参数 DTO。
 * <p>
 * 说明：讲义里这个类是当「分页查询」的骨架给的，带 page / pageSize 两个字段，
 * 但一直没人引用（`GET /admin/orders` 从来没接它）。这次把筛选条件接上了，
 * page / pageSize 两个字段**删掉了**，原因是：
 * <ul>
 *   <li>管理端的订单表由前端分页（UserManage / VerifyManage / AppealManage 都是这个做法），
 *       服务端再分一次就成了两层分页，前端算出来的总页数会和实际返回的行数对不上；</li>
 *   <li>接口的返回体是 {@code List<Orders>}，没有 total 字段 —— 真要在服务端分页，
 *       就得改返回结构，`Orders.vue` 和 tests/ 里那一套 pytest 用例都要跟着改，
 *       而需求（3.5.2）要的是「多条件筛选」，不是分页。</li>
 * </ul>
 * 留着两个永远为 null 的字段只会让后面看代码的人以为分页是好的。
 */
@Data
public class AdminOrderQuery {

    /** 订单状态：0 待支付 / 1 待接单 / 2 进行中 / 3 待确认 / 4 已完成 / 5 已取消 / 6 申诉中。不传表示全部 */
    private Integer status;

    /**
     * 关键词。匹配订单号、任务类型、任务描述、取货地点、送达地点，
     * 以及发布者 / 跑腿员的昵称、真实姓名、学号（后面三个要先到 users 里把 id 捞出来）。
     */
    private String keyword;
}
