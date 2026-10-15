package com.example.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.campus.dto.AppealCreateRequest;
import com.example.campus.dto.AppealHandleRequest;
import com.example.campus.dto.AppealVO;
import com.example.campus.entity.Appeals;

import java.util.List;

/**
 * <p>
 * 申诉仲裁 服务类（需求 3.5.3 纠纷仲裁中心）
 * </p>
 *
 * 状态机沿用 appeals 表现有的语义，没有新增取值：
 *   appeals.status 0 = 待处理，1 = 已处理（终态）
 *   orders.appeal_status 0 = 无申诉，1 = 有进行中的申诉，2 = 已仲裁
 *
 * 「有没有待处理的申诉」一律以 appeals.status = 0 为准。orders.appeal_status 只是
 * 列表页的展示位 —— campus.sql 里这两处种子数据本身就是对不上的（订单 11 挂着两条
 * 待处理申诉，appeal_status 却是 0），拿它当判定依据会漏单。
 */
public interface IAppealsService extends IService<Appeals> {

    /** 用户提交申诉：校验订单状态与当事人身份，写 appeals 并把订单转入「申诉中」 */
    AppealVO createAppeal(Long userId, AppealCreateRequest request);

    /** 我提交的申诉（移动端回看仲裁结果用） */
    List<AppealVO> listMyAppeals(Long userId);

    /** 管理端申诉列表，status 为空表示查全部，keyword 匹配订单号 / 任务类型 / 双方昵称 */
    List<AppealVO> listAppeals(Integer status, String keyword);

    /** 申诉详情 */
    AppealVO getAppealDetail(Long appealId);

    /** 仲裁处理：在一个事务里完成退款 / 补偿 / 封号 / 驳回，并落仲裁记录 */
    AppealVO handle(Long appealId, Long adminId, AppealHandleRequest request);
}
