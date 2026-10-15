package com.example.campus.controller;

import com.example.campus.common.BusinessException;
import com.example.campus.common.CsvExporter;
import com.example.campus.dto.Result;
import com.example.campus.entity.WalletTransaction;
import com.example.campus.service.IWalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理端 - 财务管理（需求 3.5.4）。
 *
 * 三件事：看平台流水汇总、审核用户提交的提现申请、做基础的财务统计。
 * 权限校验沿用 AdminController 的 requireAdmin。
 */
@RestController
@RequestMapping("/admin/finance")
@RequiredArgsConstructor
public class AdminFinanceController {

    private final IWalletService walletService;

    /** 财务统计：各类型流水的笔数与金额、待审核提现数量 */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(walletService.statistics());
    }

    /** 营收趋势：period 取 day（最近 7 天，默认）/ week（最近 6 周）/ month（最近 6 个月） */
    @GetMapping("/revenue")
    public Result<List<Map<String, Object>>> revenue(@RequestParam(required = false) String period,
                                                     HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(walletService.revenueTrend(period));
    }

    /** 提现申请列表，status 不传表示查全部（0 待审核 / 1 已通过 / 2 已驳回） */
    @GetMapping("/withdrawals")
    public Result<List<WalletTransaction>> withdrawals(@RequestParam(required = false) Integer status,
                                                       HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(walletService.listWithdrawals(status));
    }

    /** 审核提现：pass=true 通过（钱真正转出），pass=false 驳回（退回用户可用余额） */
    @PostMapping("/withdrawals/{id}/audit")
    public Result<WalletTransaction> audit(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("userId");

        Object passValue = body.get("pass");
        if (passValue == null) {
            throw new BusinessException("请指定审核结果 pass=true/false");
        }
        boolean pass = Boolean.parseBoolean(String.valueOf(passValue));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));

        return Result.success(walletService.auditWithdraw(adminId, id, pass, remark));
    }

    /**
     * 导出财务报表（需求 3.5.4）：全部资金流水明细，直接下载 CSV。
     * <p>
     * 为什么导流水明细而不是那几个统计数字：统计汇总在页面上已经看得到了，
     * 导出的意义是能拿去核对每一笔钱的来龙去脉。口径说明（平台不抽成、金额一律存正数、
     * 方向看「收支」列）都写在表头里了，看表的人不用再回来翻代码。
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFinance(HttpServletRequest request) {
        requireAdmin(request);

        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> tx : walletService.transactionReport()) {
            List<Object> row = new ArrayList<>(9);
            row.add(tx.get("createdAt"));
            row.add(tx.get("transactionNo"));
            row.add(tx.get("typeName"));
            row.add(tx.get("direction"));
            row.add(tx.get("amount"));
            row.add(tx.get("user"));
            row.add(tx.get("orderNo"));
            row.add(tx.get("status"));
            row.add(tx.get("remark"));
            rows.add(row);
        }

        List<String> headers = List.of("时间", "交易号", "类型", "收支", "金额(元)",
                "用户", "关联订单号", "状态", "备注");

        String filename = "资金流水报表" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
        return CsvExporter.download(filename, CsvExporter.toCsvBytes(headers, rows));
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
