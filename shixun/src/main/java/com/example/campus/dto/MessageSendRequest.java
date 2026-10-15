package com.example.campus.dto;

import lombok.Data;

/**
 * 发聊天消息的请求体。
 *
 * 故意只有 orderId 和 content：发送人一律从 token 里取，接收方由后端按订单双方反推。
 * 前端传 senderId / receiverId 都不接受 —— 否则等于允许冒充别人发消息。
 */
@Data
public class MessageSendRequest {

    private Long orderId;

    private String content;
}
