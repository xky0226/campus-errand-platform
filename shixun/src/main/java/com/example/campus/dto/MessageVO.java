package com.example.campus.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息的展示对象（移动端订单聊天 + 管理端仲裁里查看聊天记录，共用同一个）。
 *
 * 为什么不直接返回 Message 实体：消息列表里光有 sender_id 这个数字没法看，
 * 前端要显示「这话是谁说的」。带上 senderName 一次返回，省得前端拿 id 再查用户。
 *
 * 故意不带「这条是不是我发的」这种字段 —— 那是看的人决定的：
 * 前端拿 senderId 和自己存的 userId 比一下就行，后端不替前端判视角。
 */
@Data
public class MessageVO {

    private Long id;

    private Long orderId;

    //========== 发送方 ==========
    private Long senderId;
    private String senderName;

    //========== 接收方 ==========
    private Long receiverId;

    //========== 内容 ==========
    private String content;
    /** 目前只有 text */
    private String type;
    /** 0 未读 / 1 已读，是「接收方」的已读状态 */
    private Integer isRead;

    private LocalDateTime createdAt;
}
