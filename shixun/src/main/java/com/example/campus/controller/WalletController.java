package com.example.campus.controller;

import com.example.campus.dto.Result;
import com.example.campus.dto.WalletAmountRequest;
import com.example.campus.entity.WalletTransaction;
import com.example.campus.service.IWalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 个人钱包接口（需求 3.5 移动端前台功能 5「个人钱包：交易记录、提现、充值」）。
 *
 * 用户身份同样取自 JWT 拦截器塞进 request 的 userId 属性，
 * 不接受前端传 userId —— 否则谁都能查别人的钱包。
 */
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final IWalletService walletService;

    /** 余额概览：可用余额 / 冻结金额 / 累计收益 */
    @GetMapping("/balance")
    public Result<Map<String, Object>> getBalance(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(walletService.getBalance(userId));
    }

    /** 交易记录（流水），可按类型筛选 */
    @GetMapping("/transactions")
    public Result<List<WalletTransaction>> getTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(walletService.listTransactions(userId, type, limit));
    }

    /** 充值（模拟）：没有接真实支付渠道，调用成功即到账 */
    @PostMapping("/recharge")
    public Result<WalletTransaction> recharge(@RequestBody WalletAmountRequest body,
                                              HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(walletService.recharge(userId, body.getAmount(), body.getPayMethod()));
    }

    /** 申请提现：生成一条待审核流水，等管理员在财务模块审核 */
    @PostMapping("/withdraw")
    public Result<WalletTransaction> withdraw(@RequestBody WalletAmountRequest body,
                                              HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(walletService.withdraw(userId, body.getAmount(), body.getAccount()));
    }
}
