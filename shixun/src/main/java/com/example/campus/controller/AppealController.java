package com.example.campus.controller;

import com.example.campus.dto.AppealCreateRequest;
import com.example.campus.dto.AppealVO;
import com.example.campus.dto.Result;
import com.example.campus.service.IAppealsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 移动端 - 订单仲裁（需求 3.5.3 的用户侧入口）。
 *
 * 用户对某个订单不满，可以在这里提交申诉；提交后订单会变成「申诉中」，
 * 等管理员在管理端「申诉仲裁」里处理。这两个接口都要登录。
 */
@RestController
@RequestMapping("/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final IAppealsService appealsService;

    /** 提交申诉。申诉人 / 被申诉人由后端按订单反推，前端只需要传订单号、理由和证据图 */
    @PostMapping
    public Result<AppealVO> create(@RequestBody AppealCreateRequest request,
                                   HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(appealsService.createAppeal(userId, request));
    }

    /** 我提交过的申诉（回看仲裁结果） */
    @GetMapping("/my")
    public Result<List<AppealVO>> my(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(appealsService.listMyAppeals(userId));
    }
}
