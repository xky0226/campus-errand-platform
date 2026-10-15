package com.example.campus.controller;

import com.example.campus.dto.MessageSendRequest;
import com.example.campus.dto.MessageVO;
import com.example.campus.dto.Result;
import com.example.campus.service.IMessagesService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 移动端 - 订单聊天（需求：移动端「发布者和需求者的聊天界面」）。
 *
 * 订单被接单之后，发布者和跑腿员可以在这里沟通取件、送达这些事；
 * 管理端仲裁时也能调 /admin/appeals/{id}/messages 看到同一段对话（需求 3.5.3 的「查看聊天记录」）。
 *
 * 只做文字消息 + 前端每 3 秒轮询一次，没有 WebSocket 推送 —— 最小可用版。
 * 这两个接口都要登录，身份取自 JWT 拦截器塞进 request 的 userId。
 */
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final IMessagesService messagesService;

    /** 拉某个订单的聊天记录，顺手把发给我的未读标成已读 */
    @GetMapping
    public Result<List<MessageVO>> list(@RequestParam Long orderId, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(messagesService.listByOrder(orderId, userId));
    }

    /** 发一条消息。发送人从 token 取，接收方由后端按订单反推 */
    @PostMapping
    public Result<MessageVO> send(@RequestBody MessageSendRequest request,
                                  HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(messagesService.send(request.getOrderId(), userId, request.getContent()));
    }
}
