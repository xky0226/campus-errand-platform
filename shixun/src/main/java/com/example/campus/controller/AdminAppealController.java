package com.example.campus.controller;

import com.example.campus.common.BusinessException;
import com.example.campus.dto.AppealHandleRequest;
import com.example.campus.dto.AppealVO;
import com.example.campus.dto.MessageVO;
import com.example.campus.dto.Result;
import com.example.campus.service.IAppealsService;
import com.example.campus.service.IMessagesService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 - 纠纷仲裁中心（需求 3.5.3）。
 *
 * 路径对齐 permissions 表里写死的 admin:appeal:handle → /admin/appeals/*\/handle，
 * 这样权限表里的记录和实际接口是对得上的。
 */
@RestController
@RequestMapping("/admin/appeals")
@RequiredArgsConstructor
public class AdminAppealController {

    private final IAppealsService appealsService;

    /** 仲裁时要看聊天记录，所以这里也依赖消息服务 */
    private final IMessagesService messagesService;

    /** 申诉列表。status 不传查全部（0 待处理 / 1 已处理），keyword 匹配订单号、任务类型、双方昵称 */
    @GetMapping
    public Result<List<AppealVO>> list(@RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String keyword,
                                       HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(appealsService.listAppeals(status, keyword));
    }

    /** 申诉详情：含证据图、订单快照和处理记录 */
    @GetMapping("/{id}")
    public Result<AppealVO> detail(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(appealsService.getAppealDetail(id));
    }

    /**
     * 查看该申诉对应订单的聊天记录（需求 3.5.3「查看聊天记录与证据」）。
     *
     * 顺着 申诉 → orderId → 消息 走，所以挂在 /{id}/messages 下，不另开 Controller。
     * 这里走 listByOrderForAdmin 不校验当事人 —— 管理员本来就不是订单双方，
     * 该不该看由上面的 requireAdmin 决定。
     */
    @GetMapping("/{id}/messages")
    public Result<List<MessageVO>> messages(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        AppealVO appeal = appealsService.getAppealDetail(id);
        return Result.success(messagesService.listByOrderForAdmin(appeal.getOrderId()));
    }

    /**
     * 仲裁处理。四个动作可组合：
     * REFUND 强制退款 / COMPENSATE 补偿 / BAN 封号 / REJECT 驳回申诉（与其它三个互斥）。
     */
    @PostMapping("/{id}/handle")
    public Result<AppealVO> handle(@PathVariable Long id,
                                   @RequestBody AppealHandleRequest body,
                                   HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("userId");
        return Result.success(appealsService.handle(id, adminId, body));
    }

    /** 管理端接口统一走这个：JWT 拦截器已经把 role 塞进 request 了 */
    private void requireAdmin(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        //管理员角色role为2、0为需求者、1为跑腿员
        if (role == null || role != 2) {
            throw new BusinessException("无管理员权限");
        }
    }
}
