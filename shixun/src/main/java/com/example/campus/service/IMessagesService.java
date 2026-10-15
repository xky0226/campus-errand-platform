package com.example.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.campus.dto.MessageVO;
import com.example.campus.entity.Message;

import java.util.List;

/**
 * <p>
 * 聊天消息 服务类
 * </p>
 *
 * 只做文字消息 + 前端定时轮询，没有 WebSocket 推送 —— 最小可用版。
 */
public interface IMessagesService extends IService<Message> {

    /**
     * 拉某个订单的聊天记录（按时间升序）。
     * 会先校验当前用户是不是这个订单的当事人，顺手把「发给我的」未读消息标成已读。
     */
    List<MessageVO> listByOrder(Long orderId, Long userId);

    /** 发一条消息。接收方由后端按订单双方反推，前端说了不算 */
    MessageVO send(Long orderId, Long senderId, String content);

    /** 管理端只读查看某个订单的聊天记录，不校验当事人身份（由 Controller 保证调用者是管理员） */
    List<MessageVO> listByOrderForAdmin(Long orderId);
}
