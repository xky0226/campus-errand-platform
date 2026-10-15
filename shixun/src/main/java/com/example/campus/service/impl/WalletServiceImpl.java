package com.example.campus.service.impl;

import com.example.campus.common.BusinessException;
import com.example.campus.entity.Orders;
import com.example.campus.entity.Users;
import com.example.campus.entity.WalletTransaction;
import com.example.campus.mapper.OrdersMapper;
import com.example.campus.mapper.UsersMapper;
import com.example.campus.mapper.WalletTransactionsMapper;
import com.example.campus.service.IWalletService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * <p>
 * 钱包 服务实现类
 * </p>
 *
 * 资金口径说明（三列都在 users 表上）：
 *   wallet_balance  可用余额 —— 能直接提现、能付订单
 *   frozen_balance  冻结余额 —— 已支付但订单还没完成的钱（平台托管），提现申请待审核的钱也放这里
 *   total_earnings  累计收益 —— 跑腿员历史收入合计，只增不减，纯统计用
 *
 * @author coder
 * @since 2026-10-15
 */
@Slf4j
@Service
public class WalletServiceImpl extends ServiceImpl<WalletTransactionsMapper, WalletTransaction>
        implements IWalletService {

    @Autowired
    WalletTransactionsMapper txMapper;

    @Autowired
    UsersMapper userMapper;

    /** 导出财务报表时要把 wallet_transactions.order_id 翻成订单号给人看 */
    @Autowired
    OrdersMapper ordersMapper;

    /** 充值限额 */
    private static final BigDecimal MAX_RECHARGE = new BigDecimal("5000");
    /** 最低提现金额 */
    private static final BigDecimal MIN_WITHDRAW = new BigDecimal("10");

    /**
     * 收支方向表。金额在库里统一存正数，靠 type 区分，前端拿到 direction 直接决定显示 + 还是 −。
     *
     * 注意：Map.of 是定长的，这里漏一个 type 就会走到 directionOf 的兜底分支，
     * 钱包页会把收款（补偿）显示成支出。新增流水类型时必须同步改这张表、statistics()
     * 里的类型清单、typeName() 三处，一处漏了新流水就不进统计或者没有中文名。
     */
    private static final Map<String, Integer> DIRECTION = Map.of(
            "recharge", 1,
            "order_earn", 1,
            "order_refund", 1,
            "compensate", 1,
            "order_pay", -1,
            "withdraw", -1,
            "penalty", -1);

    // ============================ 查询 ============================

    @Override
    public Map<String, Object> getBalance(Long userId) {
        Users user = requireUser(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("balance", nz(user.getWalletBalance()));
        data.put("frozenBalance", nz(user.getFrozenBalance()));
        data.put("totalEarnings", nz(user.getTotalEarnings()));
        // 有绑定支付宝账号的话回显到提现弹窗里，省得每次重填
        data.put("alipayAccount", user.getAlipayAccount());
        return data;
    }

    @Override
    public List<WalletTransaction> listTransactions(Long userId, String type, Integer limit) {
        LambdaQueryWrapper<WalletTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WalletTransaction::getUserId, userId)
                .eq(StringUtils.hasText(type), WalletTransaction::getType, type)
                .orderByDesc(WalletTransaction::getCreatedAt)
                .orderByDesc(WalletTransaction::getId);
        // limit 只允许 1~200，防止前端传个负数或超大值
        int size = limit == null ? 50 : Math.min(Math.max(limit, 1), 200);
        wrapper.last("LIMIT " + size);

        List<WalletTransaction> list = txMapper.selectList(wrapper);
        list.forEach(tx -> tx.setDirection(directionOf(tx.getType())));
        return list;
    }

    // ============================ 充值 / 提现 ============================

    @Override
    @Transactional
    public WalletTransaction recharge(Long userId, BigDecimal amount, String payMethod) {
        Users user = requireUser(userId);
        BigDecimal money = normalizeAmount(amount, "充值金额");
        if (money.compareTo(MAX_RECHARGE) > 0) {
            throw new BusinessException("单次充值不能超过 " + MAX_RECHARGE.stripTrailingZeros().toPlainString() + " 元");
        }
        // 这里没有接真实支付渠道，是按「模拟充值」做的：点了就到账，
        // 所以流水直接记成已完成(status=1)，alipay_trade_no 也就一直是 null。
        String method = StringUtils.hasText(payMethod) ? payMethod : "支付宝";

        BigDecimal oldBalance = nz(user.getWalletBalance());
        BigDecimal oldFrozen = nz(user.getFrozenBalance());
        BigDecimal newBalance = oldBalance.add(money);

        casBalance(userId, oldBalance, oldFrozen, newBalance, oldFrozen);

        return record(userId, money, "recharge", null, 1,
                method + "充值（模拟）", newBalance);
    }

    @Override
    @Transactional
    public WalletTransaction withdraw(Long userId, BigDecimal amount, String account) {
        Users user = requireUser(userId);
        BigDecimal money = normalizeAmount(amount, "提现金额");
        if (money.compareTo(MIN_WITHDRAW) < 0) {
            throw new BusinessException("单次提现不能低于 " + MIN_WITHDRAW.stripTrailingZeros().toPlainString() + " 元");
        }
        if (!StringUtils.hasText(account)) {
            throw new BusinessException("请填写收款账号");
        }

        BigDecimal oldBalance = nz(user.getWalletBalance());
        BigDecimal oldFrozen = nz(user.getFrozenBalance());
        if (oldBalance.compareTo(money) < 0) {
            throw new BusinessException("可用余额不足，当前余额 " + oldBalance.toPlainString() + " 元");
        }
        // 申请提现先把钱从可用挪到冻结：这笔钱还在用户账上，但不能再拿去付订单
        BigDecimal newBalance = oldBalance.subtract(money);
        BigDecimal newFrozen = oldFrozen.add(money);

        casBalance(userId, oldBalance, oldFrozen, newBalance, newFrozen);

        // 顺手记下收款账号，下次提现自动带出来
        if (!account.equals(user.getAlipayAccount())) {
            Users patch = new Users();
            patch.setId(userId);
            patch.setAlipayAccount(account);
            userMapper.updateById(patch);
        }

        // 收款账号只写进 remark，不写 third_party_account。
        // 那个列在 DDL 里是 varchar(10)，连手机号（11 位）都装不下，
        // 严格模式下写进去直接抛 Data too long —— 试过了，整笔提现会回滚。
        // 管理端「提现账号」列改成从 remark 里解析（见 WithdrawManage.vue 的 accountOf）。
        return record(userId, money, "withdraw", null, 0,
                "提现到" + account, newBalance);
    }

    // ============================ 订单资金：支付 / 结算 / 退款 ============================

    @Override
    @Transactional
    public void payForOrder(Long publisherId, Long orderId, BigDecimal amount) {
        Users user = requireUser(publisherId);
        BigDecimal money = normalizeAmount(amount, "订单金额");

        BigDecimal oldBalance = nz(user.getWalletBalance());
        BigDecimal oldFrozen = nz(user.getFrozenBalance());
        if (oldBalance.compareTo(money) < 0) {
            throw new BusinessException("钱包余额不足，当前余额 " + oldBalance.toPlainString()
                    + " 元，订单需要 " + money.toPlainString() + " 元，请先充值");
        }
        // 支付只是把钱冻结起来，订单确认完成后才打给跑腿员 —— 相当于平台托管
        BigDecimal newBalance = oldBalance.subtract(money);
        BigDecimal newFrozen = oldFrozen.add(money);

        casBalance(publisherId, oldBalance, oldFrozen, newBalance, newFrozen);

        record(publisherId, money, "order_pay", orderId, 1, "订单支付", newBalance);
    }

    @Override
    @Transactional
    public void settleToRunner(Long publisherId, Long runnerId, Long orderId, BigDecimal amount) {
        if (!hasFrozenForOrder(orderId)) {
            return;
        }
        BigDecimal money = normalizeAmount(amount, "结算金额");

        // 1. 从发布者的冻结余额里把这笔托管资金扣掉
        Users publisher = requireUser(publisherId);
        BigDecimal pOldBalance = nz(publisher.getWalletBalance());
        BigDecimal pOldFrozen = nz(publisher.getFrozenBalance());
        if (pOldFrozen.compareTo(money) < 0) {
            throw new BusinessException("订单托管资金异常，发布者冻结余额不足");
        }
        casBalance(publisherId, pOldBalance, pOldFrozen, pOldBalance, pOldFrozen.subtract(money));

        // 2. 打给跑腿员，同时累加累计收益
        Users runner = requireUser(runnerId);
        BigDecimal rOldBalance = nz(runner.getWalletBalance());
        BigDecimal rOldFrozen = nz(runner.getFrozenBalance());
        BigDecimal rNewBalance = rOldBalance.add(money);
        casBalance(runnerId, rOldBalance, rOldFrozen, rNewBalance, rOldFrozen);

        Users patch = new Users();
        patch.setId(runnerId);
        patch.setTotalEarnings(nz(runner.getTotalEarnings()).add(money));
        patch.setCompletedOrders((runner.getCompletedOrders() == null ? 0 : runner.getCompletedOrders()) + 1);
        userMapper.updateById(patch);

        record(runnerId, money, "order_earn", orderId, 1, "订单收益", rNewBalance);
    }

    @Override
    @Transactional
    public void refundToPublisher(Long publisherId, Long orderId, BigDecimal amount, String remark) {
        if (!hasFrozenForOrder(orderId)) {
            return;
        }
        Users user = requireUser(publisherId);
        BigDecimal money = normalizeAmount(amount, "退款金额");

        BigDecimal oldBalance = nz(user.getWalletBalance());
        BigDecimal oldFrozen = nz(user.getFrozenBalance());
        if (oldFrozen.compareTo(money) < 0) {
            throw new BusinessException("订单托管资金异常，冻结余额不足，无法退款");
        }
        BigDecimal newBalance = oldBalance.add(money);
        BigDecimal newFrozen = oldFrozen.subtract(money);

        casBalance(publisherId, oldBalance, oldFrozen, newBalance, newFrozen);

        record(publisherId, money, "order_refund", orderId, 1,
                StringUtils.hasText(remark) ? remark : "订单退款", newBalance);
    }

    // ============================ 申诉仲裁：托管余额 / 补偿 ============================

    @Override
    public BigDecimal escrowRemaining(Long orderId) {
        if (orderId == null) {
            return BigDecimal.ZERO;
        }
        List<WalletTransaction> list = txMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOrderId, orderId)
                .eq(WalletTransaction::getStatus, 1));

        // 这里跟 statistics() 一样是全量捞出来在 Java 里累加，不写 SQL 聚合 ——
        // 一张订单的流水撑死几条，没必要为它再往 Mapper 上加一个 @Select，
        // 口径也跟 statistics() 保持一致，好对账。
        BigDecimal remain = BigDecimal.ZERO;
        for (WalletTransaction tx : list) {
            BigDecimal amount = nz(tx.getAmount());
            switch (tx.getType() == null ? "" : tx.getType()) {
                case "order_pay" -> remain = remain.add(amount);          // 支付：钱进了托管
                case "order_refund", "order_earn" -> remain = remain.subtract(amount); // 退款/结算：钱出了托管
                default -> {
                    // 其它类型（充值、提现、仲裁补偿）跟订单托管无关，不计入
                }
            }
        }
        return remain.max(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public WalletTransaction compensate(Long fromUserId, Long toUserId, Long orderId,
                                        BigDecimal amount, String remark) {
        if (fromUserId == null || toUserId == null) {
            throw new BusinessException("补偿的收付双方不能为空");
        }
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException("扣款方和收款方不能是同一个用户");
        }
        BigDecimal money = normalizeAmount(amount, "补偿金额");
        String memo = StringUtils.hasText(remark) ? remark : "仲裁补偿";

        // 1. 从责任方的可用余额里扣。余额不足直接抛异常，不做部分扣款 ——
        //    部分扣款会让「判罚金额」和「实际到账」对不上，对账时说不清楚。
        Users from = requireUser(fromUserId);
        BigDecimal fromBalance = nz(from.getWalletBalance());
        BigDecimal fromFrozen = nz(from.getFrozenBalance());
        if (fromBalance.compareTo(money) < 0) {
            throw new BusinessException("责任方可用余额不足（当前 " + fromBalance.toPlainString()
                    + " 元），无法执行 " + money.toPlainString() + " 元补偿，请降低补偿金额或改用其它仲裁操作");
        }
        BigDecimal fromNew = fromBalance.subtract(money);
        casBalance(fromUserId, fromBalance, fromFrozen, fromNew, fromFrozen);
        // 扣款方记一条 penalty（支出），收款方记一条 compensate（收入），两边都有据可查
        record(fromUserId, money, "penalty", orderId, 1, memo + "（扣款方）", fromNew);

        // 2. 加到受损方的可用余额
        Users to = requireUser(toUserId);
        BigDecimal toBalance = nz(to.getWalletBalance());
        BigDecimal toFrozen = nz(to.getFrozenBalance());
        BigDecimal toNew = toBalance.add(money);
        casBalance(toUserId, toBalance, toFrozen, toNew, toFrozen);
        return record(toUserId, money, "compensate", orderId, 1, memo + "（收款方）", toNew);
    }

    // ============================ 管理端：提现审核 / 财务统计 ============================

    @Override
    public List<WalletTransaction> listWithdrawals(Integer status) {
        LambdaQueryWrapper<WalletTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WalletTransaction::getType, "withdraw")
                .eq(status != null, WalletTransaction::getStatus, status)
                .orderByDesc(WalletTransaction::getCreatedAt);
        List<WalletTransaction> list = txMapper.selectList(wrapper);
        // 带上申请人的昵称，管理端表格直接显示，不用再一个个查用户
        for (WalletTransaction tx : list) {
            Users u = userMapper.selectById(tx.getUserId());
            if (u != null) {
                // 用 nzRemark 而不是直接拼：remark 有可能是 null，
                // 直接拼会显示成「null（张三）」。
                tx.setRemark(nzRemark(tx.getRemark()) + "（" + u.getNickname() + "）");
            }
            tx.setDirection(-1);
        }
        return list;
    }

    @Override
    @Transactional
    public WalletTransaction auditWithdraw(Long adminId, Long txId, boolean pass, String remark) {
        WalletTransaction tx = txMapper.selectById(txId);
        if (tx == null || tx.getDeleted() != 0) {
            throw new BusinessException("提现申请不存在");
        }
        if (!"withdraw".equals(tx.getType())) {
            throw new BusinessException("该流水不是提现申请");
        }
        if (!Integer.valueOf(0).equals(tx.getStatus())) {
            throw new BusinessException("该提现申请已经处理过了");
        }

        Users user = requireUser(tx.getUserId());
        BigDecimal amount = nz(tx.getAmount());
        BigDecimal oldBalance = nz(user.getWalletBalance());
        BigDecimal oldFrozen = nz(user.getFrozenBalance());
        if (oldFrozen.compareTo(amount) < 0) {
            throw new BusinessException("该用户冻结金额不足，无法处理");
        }

        BigDecimal newBalance;
        if (pass) {
            // 审核通过：钱真的转出去了，冻结余额直接扣掉，可用余额不动
            newBalance = oldBalance;
        } else {
            // 审核驳回：钱退回可用余额
            newBalance = oldBalance.add(amount);
        }
        casBalance(tx.getUserId(), oldBalance, oldFrozen, newBalance, oldFrozen.subtract(amount));

        WalletTransaction update = new WalletTransaction();
        update.setId(txId);
        update.setStatus(pass ? 1 : 2);
        String suffix = pass ? "审核通过" : "审核驳回";
        update.setRemark(nzRemark(tx.getRemark()) + " —— " + suffix
                + (StringUtils.hasText(remark) ? "：" + remark : "")
                + "（管理员 " + adminId + " 处理）");
        txMapper.updateById(update);

        tx.setStatus(update.getStatus());
        tx.setRemark(update.getRemark());
        return tx;
    }

    @Override
    public Map<String, Object> statistics() {
        List<WalletTransaction> all = txMapper.selectList(new LambdaQueryWrapper<>());

        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        // 这份清单是统计的白名单：不在里面的类型会被整个跳过、不进任何统计。
        // 新增流水类型（比如仲裁的 penalty / compensate）必须同步加进来。
        for (String type : List.of("recharge", "order_pay", "order_earn", "order_refund", "withdraw",
                "penalty", "compensate")) {
            counts.put(type, 0);
            sums.put(type, BigDecimal.ZERO);
        }
        for (WalletTransaction tx : all) {
            // 只有已完成的流水才计入统计，待审核的提现不算
            if (!Integer.valueOf(1).equals(tx.getStatus())) {
                continue;
            }
            String type = tx.getType();
            if (counts.containsKey(type)) {
                counts.put(type, counts.get(type) + 1);
                sums.put(type, sums.get(type).add(nz(tx.getAmount())));
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String type : counts.keySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", type);
            row.put("typeName", typeName(type));
            row.put("count", counts.get(type));
            row.put("amount", sums.get(type));
            rows.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", rows);
        data.put("totalRecharge", sums.get("recharge"));
        data.put("totalPay", sums.get("order_pay"));
        data.put("totalEarn", sums.get("order_earn"));
        data.put("totalWithdraw", sums.get("withdraw"));
        // 这里没有单独统计「平台抽成」：发布订单时 orders.platform_fee 写的就是 0，
        // 平台目前不抽成，赏金全额结算给跑腿员，硬凑一个抽成数字反而是假的。
        data.put("pendingWithdrawCount", all.stream()
                .filter(tx -> "withdraw".equals(tx.getType()) && Integer.valueOf(0).equals(tx.getStatus()))
                .count());
        return data;
    }

    @Override
    public List<Map<String, Object>> transactionReport() {
        List<WalletTransaction> all = txMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                .orderByDesc(WalletTransaction::getCreatedAt)
                .orderByDesc(WalletTransaction::getId));

        // 先把这一批流水涉及的用户和订单一次性查出来做成 Map。
        // 逐行去 selectById 就是 N+1（一个导出接口几百条流水直接几百次查询），
        // 而且事务里循环查库也很容易把连接池占满。
        Set<Long> userIds = all.stream().map(WalletTransaction::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> orderIds = all.stream().map(WalletTransaction::getOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> nicknames = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Users u : userMapper.selectList(new LambdaQueryWrapper<Users>()
                    .select(Users::getId, Users::getNickname)
                    .in(Users::getId, userIds))) {
                nicknames.put(u.getId(), u.getNickname() == null ? "" : u.getNickname());
            }
        }

        Map<Long, String> orderNos = new HashMap<>();
        if (!orderIds.isEmpty()) {
            for (Orders o : ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                    .select(Orders::getId, Orders::getOrderNo)
                    .in(Orders::getId, orderIds))) {
                orderNos.put(o.getId(), o.getOrderNo() == null ? "" : o.getOrderNo());
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>(all.size());
        for (WalletTransaction tx : all) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("createdAt", tx.getCreatedAt());
            row.put("transactionNo", tx.getTransactionNo());
            // 用 typeName() 而不是直接吐 type：流水类型是英文标识符，
            // 报表是给人看的，不能只报 recharge / order_pay
            row.put("typeName", typeName(tx.getType()));
            row.put("direction", directionOf(tx.getType()) > 0 ? "收入" : "支出");
            // 金额在库里统一存正数，方向由上面那列表达（和钱包页的口径一致）
            row.put("amount", nz(tx.getAmount()));
            row.put("user", nicknames.getOrDefault(tx.getUserId(), ""));
            row.put("orderNo", orderNos.getOrDefault(tx.getOrderId(), ""));
            // 0 是「处理中」，只有提现申请会出现；其余类型落库就是 1
            row.put("status", Integer.valueOf(1).equals(tx.getStatus()) ? "已完成" : "处理中");
            row.put("remark", nzRemark(tx.getRemark()));
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> revenueTrend(String period) {
        String p = StringUtils.hasText(period) ? period.trim().toLowerCase() : "day";

        // 先按时间倒着推出要展示的桶（键 + 显示名），没有交易也留着，最后补 0
        LinkedHashMap<String, String> buckets = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        if (p.startsWith("month")) {
            for (int i = 5; i >= 0; i--) {
                YearMonth ym = YearMonth.from(today).minusMonths(i);
                buckets.put(ym.toString(), ym.getMonthValue() + "月");      // 2026-10 → 10月
            }
        } else if (p.startsWith("week")) {
            for (int i = 5; i >= 0; i--) {
                LocalDate monday = today.with(DayOfWeek.MONDAY).minusWeeks(i);
                buckets.put(monday.toString(), monday.getMonthValue() + "/" + monday.getDayOfMonth());
            }
        } else {
            p = "day";
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                buckets.put(d.toString(), d.getMonthValue() + "/" + d.getDayOfMonth());
            }
        }

        // 口径跟 statistics() 一致：只统计已完成的流水，而且只算 order_pay 的交易额。
        // amount > 0 这个条件不能省 —— campus.sql 里有几条 amount 是负数的 order_pay
        // （-5.00，order_id 为 null），不过滤的话某天的营收会莫名其妙变成负数。
        // 平台目前不抽成（orders.platform_fee 恒为 0），所以这里的「营收」是平台上的
        // 交易流水额，不是平台自己的收入，财务页上要写清楚。
        List<WalletTransaction> all = txMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getType, "order_pay")
                .eq(WalletTransaction::getStatus, 1)
                .gt(WalletTransaction::getAmount, BigDecimal.ZERO));

        Map<String, Integer> counts = new HashMap<>();
        Map<String, BigDecimal> sums = new HashMap<>();
        for (WalletTransaction tx : all) {
            if (tx.getCreatedAt() == null) {
                continue;
            }
            String key = bucketKey(tx.getCreatedAt().toLocalDate(), p);
            counts.merge(key, 1, Integer::sum);
            sums.merge(key, nz(tx.getAmount()), BigDecimal::add);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, String> e : buckets.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("period", e.getKey());
            row.put("label", e.getValue());
            row.put("count", counts.getOrDefault(e.getKey(), 0));
            row.put("amount", sums.getOrDefault(e.getKey(), BigDecimal.ZERO));
            rows.add(row);
        }
        return rows;
    }

    /** 把一个日期归到对应的桶键上：day → 日期本身，week → 那周的周一，month → yyyy-MM */
    private String bucketKey(LocalDate date, String period) {
        if (period.startsWith("month")) {
            return YearMonth.from(date).toString();
        }
        if (period.startsWith("week")) {
            return date.with(DayOfWeek.MONDAY).toString();
        }
        return date.toString();
    }

    private String typeName(String type) {
        return switch (type) {
            case "recharge" -> "充值";
            case "order_pay" -> "订单支付";
            case "order_earn" -> "订单收益";
            case "order_refund" -> "订单退款";
            case "withdraw" -> "提现";
            case "penalty" -> "仲裁扣款";
            case "compensate" -> "仲裁补偿";
            default -> type;
        };
    }

    // ============================ 内部工具方法 ============================

    /**
     * 写一条流水。返回插好的实体（id 已回填），方便接口直接把这条流水返回给前端。
     *
     * @param amount   金额（正数，方向由 type 决定）
     * @param balanceAfter 变动后的可用余额，塞进 remark 里方便对账
     */
    private WalletTransaction record(Long userId, BigDecimal amount, String type, Long orderId,
                                     int status, String remark, BigDecimal balanceAfter) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setTransactionNo(nextTransactionNo());
        tx.setAmount(amount);
        tx.setType(type);
        tx.setOrderId(orderId);
        tx.setStatus(status);
        tx.setRemark(remark + "，余额 " + balanceAfter.toPlainString() + " 元");
        tx.setPayTime(LocalDateTime.now().toString());
        tx.setDirection(directionOf(type));
        txMapper.insert(tx);
        return tx;
    }

    /**
     * 原子地改余额：拿「改之前的值」当查询条件，别人同时改过就更新不到，抛异常让用户重试。
     * 相当于一次 compare-and-set，避免两个请求同时扣款把余额扣成负数。
     */
    private void casBalance(Long userId, BigDecimal oldBalance, BigDecimal oldFrozen,
                            BigDecimal newBalance, BigDecimal newFrozen) {
        int rows = userMapper.update(null, Wrappers.<Users>lambdaUpdate()
                .set(Users::getWalletBalance, newBalance)
                .set(Users::getFrozenBalance, newFrozen)
                .eq(Users::getId, userId)
                .eq(Users::getWalletBalance, oldBalance)
                .eq(Users::getFrozenBalance, oldFrozen));
        if (rows == 0) {
            throw new BusinessException("余额刚刚发生变动，请刷新后重试");
        }
    }

    /** 交易号：TX + yyyyMMddHHmmss + 6 位随机数，配合 uk_tx_no 唯一索引 */
    private String nextTransactionNo() {
        return "TX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100000, 1000000);
    }

    /** 金额校验：不能为空、必须大于 0，统一保留两位小数 */
    private BigDecimal normalizeAmount(BigDecimal amount, String label) {
        if (amount == null) {
            throw new BusinessException("请填写" + label);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(label + "必须大于 0");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private Users requireUser(Long userId) {
        Users user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /**
     * 这笔订单有没有走过钱包支付（也就是有没有被冻结的资金）。
     *
     * 加这个判断是因为 campus.sql 里那批测试订单的 payment_status 本来就是 1，
     * 但它们是「钱包打通之前」的模拟支付，用户冻结余额里其实没有对应的钱。
     * 对这类历史订单直接跳过资金处理，而不是抛异常把整个订单流程卡住。
     */
    private boolean hasFrozenForOrder(Long orderId) {
        Long count = txMapper.selectCount(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOrderId, orderId)
                .eq(WalletTransaction::getType, "order_pay")
                .eq(WalletTransaction::getStatus, 1));
        boolean exists = count != null && count > 0;
        if (!exists) {
            log.warn("订单 {} 没有对应的 order_pay 流水（钱包打通前的历史数据），跳过资金处理", orderId);
        }
        return exists;
    }

    /** 数据库里余额列可能是 null（老数据），统一当 0 处理 */
    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nzRemark(String remark) {
        return StringUtils.hasText(remark) ? remark : "";
    }

    private Integer directionOf(String type) {
        return DIRECTION.getOrDefault(type, -1);
    }
}
