package com.example.campus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.campus.common.BusinessException;
import com.example.campus.dto.MessageVO;
import com.example.campus.entity.Message;
import com.example.campus.entity.Orders;
import com.example.campus.entity.Users;
import com.example.campus.mapper.MessagesMapper;
import com.example.campus.mapper.OrdersMapper;
import com.example.campus.mapper.UsersMapper;
import com.example.campus.service.IMessagesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 聊天消息 服务实现类
 * </p>
 *
 * 数据范围靠 requireParticipant() 卡住：只有订单的发布者和跑腿员能看 / 能发，
 * 别人连这个订单有几句对话都不该知道。
 */
@Slf4j
@Service
public class MessagesServiceImpl extends ServiceImpl<MessagesMapper, Message> implements IMessagesService {

    /** 单条消息最长多少字。content 列是 text 本身不限长度，这是产品上的限制 */
    private static final int MAX_CONTENT_LENGTH = 500;

    /** 目前只发文字。表里的 type 列留着给以后做图片消息 */
    private static final String TYPE_TEXT = "text";

    @Autowired
    MessagesMapper messagesMapper;

    @Autowired
    OrdersMapper ordersMapper;

    @Autowired
    UsersMapper usersMapper;

    @Override
    public List<MessageVO> listByOrder(Long orderId, Long userId) {
        Orders order = requireParticipant(orderId, userId);
        //读完顺手把「发给我的」未读标成已读（用一条批量 UPDATE，别在循环里逐条改）
        messagesMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getOrderId, order.getId())
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
        return loadMessages(order.getId());
    }

    @Override
    public MessageVO send(Long orderId, Long senderId, String content) {
        Orders order = requireParticipant(orderId, senderId);
        //还没人接单时没有对话对象，接收方无从推断
        if (order.getRunnerId() == null) {
            throw new BusinessException("该订单还没有跑腿员接单，暂时没有可以聊天的人");
        }

        String text = content == null ? "" : content.trim();
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("消息内容不能为空");
        }
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("消息内容不能超过 " + MAX_CONTENT_LENGTH + " 个字");
        }

        //接收方 = 订单双方里的另一个人。前端传什么都不信。
        Long receiverId = senderId.equals(order.getPublisherId())
                ? order.getRunnerId()
                : order.getPublisherId();

        Message message = new Message();
        message.setOrderId(order.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(text);
        message.setType(TYPE_TEXT);
        //刚发出去的消息对对方来说是未读
        message.setIsRead(0);
        messagesMapper.insert(message);

        //insert 只回填自增主键，created_at 是数据库默认值，得重新查一次才拿得到
        Message saved = messagesMapper.selectById(message.getId());
        log.info("订单 {} 新增聊天消息 {}：{} -> {}", order.getOrderNo(), message.getId(), senderId, receiverId);
        return toVO(saved, loadNames(List.of(saved)));
    }

    @Override
    public List<MessageVO> listByOrderForAdmin(Long orderId) {
        return loadMessages(orderId);
    }

    // ============================ 私有工具 ============================

    /**
     * 校验当前用户确实是这个订单的发布者或跑腿员，然后把订单返回。
     *
     * 订单不存在（或已逻辑删除，Orders 挂了 @TableLogic 会自动过滤掉）一律按「订单不存在」处理；
     * 存在但不是当事人 —— 直接拒绝，不能让人围观别人的订单聊天。
     */
    private Orders requireParticipant(Long orderId, Long userId) {
        if (orderId == null) {
            throw new BusinessException("请指定订单");
        }
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        boolean isPublisher = userId != null && userId.equals(order.getPublisherId());
        boolean isRunner = userId != null && order.getRunnerId() != null
                && userId.equals(order.getRunnerId());
        if (!isPublisher && !isRunner) {
            throw new BusinessException("无权查看该订单的聊天记录");
        }
        return order;
    }

    /**
     * 取某订单的全部消息。
     *
     * ⚠️ 按 id 升序，不按 created_at —— 种子数据里同一个订单的几条消息
     * created_at 是一模一样的（都是导库那一刻），按时间排顺序不稳定，会串。
     */
    private List<MessageVO> loadMessages(Long orderId) {
        List<Message> list = messagesMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getOrderId, orderId)
                .orderByAsc(Message::getId));
        Map<Long, String> names = loadNames(list);
        List<MessageVO> result = new ArrayList<>();
        for (Message m : list) {
            result.add(toVO(m, names));
        }
        return result;
    }

    /** 一次 in(...) 把发送者昵称查完，别在循环里逐个 selectById */
    private Map<Long, String> loadNames(List<Message> list) {
        Set<Long> ids = new HashSet<>();
        for (Message m : list) {
            if (m.getSenderId() != null) {
                ids.add(m.getSenderId());
            }
        }
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        //users 表的 deleted 没挂 @TableLogic，必须手写 deleted = 0，否则注销用户也会被查出来
        List<Users> users = usersMapper.selectList(new LambdaQueryWrapper<Users>()
                .in(Users::getId, ids)
                .eq(Users::getDeleted, 0));
        for (Users u : users) {
            map.put(u.getId(), StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getRealName());
        }
        return map;
    }

    private MessageVO toVO(Message m, Map<Long, String> names) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setOrderId(m.getOrderId());
        vo.setSenderId(m.getSenderId());
        vo.setSenderName(names.get(m.getSenderId()));
        vo.setReceiverId(m.getReceiverId());
        vo.setContent(m.getContent());
        vo.setType(m.getType());
        vo.setIsRead(m.getIsRead());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }
}
