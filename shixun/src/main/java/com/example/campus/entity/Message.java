package com.example.campus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 聊天消息表
 * </p>
 *
 * ⚠️ 上面这个 @TableName 不能省：表名是复数 messages，MyBatis-Plus 默认按类名推表名，
 * 不写就推成单数 message，一调就 Table 'campus.message' doesn't exist。
 * （WalletTransaction 当初就是漏了同名注解炸过一次，别再来一遍。）
 *
 * @author coder
 * @since 2026-10-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("messages")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long senderId;

    private Long receiverId;

    private String content;

    /** 消息类型。目前只发 text，这列是表里本来就有的，留给以后做图片消息 */
    private String type;

    /** 0 未读 / 1 已读 —— 只对「收到这条消息的人」有意义 */
    private Integer isRead;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
