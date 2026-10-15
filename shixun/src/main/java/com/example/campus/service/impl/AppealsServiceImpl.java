package com.example.campus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.campus.common.BusinessException;
import com.example.campus.dto.AppealCreateRequest;
import com.example.campus.dto.AppealHandleRequest;
import com.example.campus.dto.AppealVO;
import com.example.campus.entity.Appeals;
import com.example.campus.entity.Orders;
import com.example.campus.entity.Users;
import com.example.campus.mapper.AppealsMapper;
import com.example.campus.mapper.OrdersMapper;
import com.example.campus.mapper.UsersMapper;
import com.example.campus.service.IAppealsService;
import com.example.campus.service.ICreditScoreService;
import com.example.campus.service.IWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 申诉仲裁 服务实现类
 * </p>
 *
 * 仲裁要同时改 appeals / orders / users / wallet_transactions 四张表，
 * 所以 handle() 整个圈在一个事务里 —— 只要中间任何一步抛异常（比如余额不足），
 * 前面抢到的仲裁权、改过的订单状态、动过的钱全部回滚，不会出现「钱退了但申诉还挂着」。
 */
@Slf4j
@Service
public class AppealsServiceImpl extends ServiceImpl<AppealsMapper, Appeals> implements IAppealsService {

    /** 只有这些订单状态可以发起申诉：0待支付/1待接单 还没人干活，5已取消 已终止，6申诉中 不可重复 */
    private static final List<Integer> APPEALABLE_STATUS = List.of(2, 3, 4);

    /** 最多带 3 张证据图 */
    private static final int MAX_EVIDENCE = 3;

    /** 判补偿时扣责任方多少信用分 */
    private static final int COMPENSATE_CREDIT_PENALTY = 5;

    private static final String ACTION_REFUND = "REFUND";
    private static final String ACTION_COMPENSATE = "COMPENSATE";
    private static final String ACTION_BAN = "BAN";
    private static final String ACTION_REJECT = "REJECT";
    private static final Set<String> ALL_ACTIONS =
            Set.of(ACTION_REFUND, ACTION_COMPENSATE, ACTION_BAN, ACTION_REJECT);

    @Autowired
    AppealsMapper appealsMapper;

    @Autowired
    OrdersMapper ordersMapper;

    @Autowired
    UsersMapper usersMapper;

    @Autowired
    IWalletService walletService;

    @Autowired
    ICreditScoreService creditScoreService;

    // ============================ 用户端：提交申诉 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppealVO createAppeal(Long userId, AppealCreateRequest request) {
        if (request == null || request.getOrderId() == null) {
            throw new BusinessException("请指定要申诉的订单");
        }
        Orders order = ordersMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getRunnerId() == null) {
            throw new BusinessException("该订单还没有跑腿员接单，没有可申诉的对象");
        }

        boolean isPublisher = userId.equals(order.getPublisherId());
        boolean isRunner = userId.equals(order.getRunnerId());
        if (!isPublisher && !isRunner) {
            throw new BusinessException("只有该订单的发布者或跑腿员才能申诉");
        }

        if (!APPEALABLE_STATUS.contains(order.getStatus())) {
            throw new BusinessException("订单当前状态不能申诉（只有进行中 / 待确认 / 已完成的订单可以申诉）");
        }

        // 一个订单同时只能有一条待处理申诉，挡住重复提交
        Long pending = appealsMapper.selectCount(Wrappers.<Appeals>lambdaQuery()
                .eq(Appeals::getOrderId, order.getId())
                .eq(Appeals::getStatus, 0));
        if (pending != null && pending > 0) {
            throw new BusinessException("该订单已有待处理的申诉，请等待管理员仲裁");
        }

        String reason = request.getReason() == null ? "" : request.getReason().trim();
        if (reason.length() < 5) {
            throw new BusinessException("请把申诉理由写清楚（至少 5 个字）");
        }
        if (reason.length() > 500) {
            throw new BusinessException("申诉理由不能超过 500 字");
        }

        List<String> evidence = normalizeEvidence(request.getEvidenceUrls());

        // 被申诉人由订单反推，不信前端传的
        Long defendantId = isPublisher ? order.getRunnerId() : order.getPublisherId();

        Appeals appeal = new Appeals();
        appeal.setOrderId(order.getId());
        appeal.setAppealerId(userId);
        appeal.setDefendantId(defendantId);
        appeal.setReason(reason);
        appeal.setEvidenceUrls(String.join(",", evidence));
        appeal.setStatus(0);
        appealsMapper.insert(appeal);

        // 订单转入「申诉中」，让发布者/跑腿员在订单列表里一眼看到状态变了
        Orders patch = new Orders();
        patch.setId(order.getId());
        patch.setStatus(6);
        patch.setAppealStatus(1);
        patch.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(patch);

        log.info("用户 {} 对订单 {} 提交申诉 appealId={}，被申诉人 {}",
                userId, order.getOrderNo(), appeal.getId(), defendantId);
        return getAppealDetail(appeal.getId());
    }

    @Override
    public List<AppealVO> listMyAppeals(Long userId) {
        List<Appeals> list = appealsMapper.selectList(Wrappers.<Appeals>lambdaQuery()
                .eq(Appeals::getAppealerId, userId)
                .orderByDesc(Appeals::getCreatedAt)
                .orderByDesc(Appeals::getId));
        return toVOs(list);
    }

    // ============================ 管理端：列表 / 详情 ============================

    @Override
    public List<AppealVO> listAppeals(Integer status, String keyword) {
        List<Appeals> list = appealsMapper.selectList(Wrappers.<Appeals>lambdaQuery()
                .eq(status != null, Appeals::getStatus, status)
                .orderByAsc(Appeals::getStatus)      // 待处理的排前面
                .orderByDesc(Appeals::getCreatedAt)
                .orderByDesc(Appeals::getId));

        List<AppealVO> vos = toVOs(list);
        if (!StringUtils.hasText(keyword)) {
            return vos;
        }
        // 关键词要匹配到订单号和双方昵称，这两个字段是拼 VO 时才带出来的，
        // 所以只能拼完在这里过滤（和 OrdersServiceImpl.listOrders 的做法一致）
        String kw = keyword.trim().toLowerCase();
        List<AppealVO> hit = new ArrayList<>();
        for (AppealVO vo : vos) {
            if (contains(vo.getOrderNo(), kw)
                    || contains(vo.getTaskType(), kw)
                    || contains(vo.getAppealerName(), kw)
                    || contains(vo.getDefendantName(), kw)) {
                hit.add(vo);
            }
        }
        return hit;
    }

    @Override
    public AppealVO getAppealDetail(Long appealId) {
        Appeals appeal = appealsMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }
        List<AppealVO> vos = toVOs(List.of(appeal));
        return vos.isEmpty() ? null : vos.get(0);
    }

    // ============================ 管理端：仲裁处理 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppealVO handle(Long appealId, Long adminId, AppealHandleRequest request) {
        if (request == null) {
            throw new BusinessException("请提交仲裁处理结果");
        }
        Set<String> actions = parseActions(request.getActions());
        if (actions.isEmpty()) {
            throw new BusinessException("请至少选择一项仲裁操作");
        }
        if (actions.size() > 1 && actions.contains(ACTION_REJECT)) {
            throw new BusinessException("「驳回申诉」表示申诉不成立，不能与其它仲裁操作同时选择");
        }

        Appeals appeal = appealsMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }

        // 【先抢占仲裁权，再做任何动钱的事】
        // 用 status=0 当条件做一次 CAS 更新：两个管理员同时点「处理」时，
        // 后到的那个更新到 0 行，在这里就被挡下来，绝不会同一笔订单退两次款。
        // 只 selectById 判一下 status 是不够的 —— 两个请求都能读到 0，然后都往下走。
        // 这一步之后的任何异常都会连这次更新一起回滚，申诉状态回到待处理，可以重试。
        int claimed = appealsMapper.update(null, Wrappers.<Appeals>lambdaUpdate()
                .set(Appeals::getStatus, 1)
                .set(Appeals::getAdminId, adminId)
                .eq(Appeals::getId, appealId)
                .eq(Appeals::getStatus, 0));
        if (claimed == 0) {
            throw new BusinessException("该申诉已被处理，请刷新列表");
        }

        Orders order = ordersMapper.selectById(appeal.getOrderId());
        if (order == null) {
            throw new BusinessException("申诉关联的订单不存在");
        }

        // 双方身份以订单为准；对不上时退回 appeals 表里存的值
        Long[] parties = resolveParties(order, appeal);
        Long appealerId = parties != null ? parties[0] : appeal.getAppealerId();
        Long defendantId = parties != null ? parties[1] : appeal.getDefendantId();

        List<String> summary = new ArrayList<>();
        // 仲裁后订单的终态；null 表示没有资金动作，还原为申诉前的状态
        Integer finalStatus = null;

        if (actions.contains(ACTION_REFUND)) {
            finalStatus = doRefund(order, summary);
        }

        if (actions.contains(ACTION_COMPENSATE)) {
            doCompensate(order, request, parties, appealerId, defendantId, summary);
            if (finalStatus == null) {
                finalStatus = 4;   // 判了补偿，视为争议处理完毕
            }
        }

        if (actions.contains(ACTION_BAN)) {
            doBan(defendantId, summary);
        }

        boolean rejected = actions.contains(ACTION_REJECT);
        if (rejected) {
            summary.add("驳回申诉：申诉不成立，订单恢复为申诉前状态");
        }

        // 订单终态：驳回 → 还原并清掉申诉标记；其余 → 有资金动作就用资金动作的终态，
        // 没有资金动作（比如只封号）也要还原，否则订单会永远卡在「申诉中」。
        patchOrder(order, rejected ? restoreStatus(order) : (finalStatus != null ? finalStatus : restoreStatus(order)),
                rejected ? 0 : 2);

        String adminRemark = String.join("；", summary);
        if (StringUtils.hasText(request.getRemark())) {
            adminRemark = adminRemark + "。管理员备注：" + request.getRemark().trim();
        }
        appealsMapper.update(null, Wrappers.<Appeals>lambdaUpdate()
                .set(Appeals::getAdminRemark, adminRemark)
                .eq(Appeals::getId, appealId));

        log.info("管理员 {} 处理申诉 {}，动作 {}，备注：{}", adminId, appealId, actions, adminRemark);
        return getAppealDetail(appealId);
    }

    /**
     * 强制退款：把订单的托管资金退回发布者，订单转「已取消」。
     *
     * 这里必须先问 escrowRemaining，不能直接调 refundToPublisher ——
     * 那个方法开头有 hasFrozenForOrder 判断，查不到 order_pay 流水就 log.warn 后直接 return，
     * 既不报错也不退钱。campus.sql 里的历史订单恰好全都没有这条流水，
     * 直接调就会出现「仲裁提示成功、实际一分钱没退」。资金不足时如实写进备注，不假装成功。
     *
     * @return 退款成功返回订单终态 5（已取消），没退成返回 null（由调用方还原订单状态）
     */
    private Integer doRefund(Orders order, List<String> summary) {
        BigDecimal refund = order.getReward() == null ? BigDecimal.ZERO : order.getReward();
        BigDecimal escrow = walletService.escrowRemaining(order.getId());

        if (escrow.compareTo(refund) < 0) {
            summary.add("强制退款：未执行 —— 该订单可支配托管资金 "
                    + escrow.toPlainString() + " 元，不足 " + refund.toPlainString()
                    + " 元（历史订单未走钱包支付，或资金已结算/已退过）");
            return null;
        }

        walletService.refundToPublisher(order.getPublisherId(), order.getId(), refund, "仲裁强制退款");
        summary.add("强制退款：已退回发布者 " + refund.toPlainString() + " 元");
        return 5;
    }

    /** 补偿：从责任方可用余额扣款赔给申诉方，并扣责任方信用分 */
    private void doCompensate(Orders order, AppealHandleRequest request, Long[] parties,
                              Long appealerId, Long defendantId, List<String> summary) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请填写大于 0 的补偿金额");
        }
        if (parties == null) {
            throw new BusinessException("这条申诉的当事人与订单对不上（历史数据），无法确定扣款方，请改用封号或驳回");
        }
        if (appealerId.equals(defendantId)) {
            throw new BusinessException("申诉方与责任方是同一个用户，无法补偿");
        }

        walletService.compensate(defendantId, appealerId, order.getId(), amount, "仲裁补偿");
        changeCredit(defendantId, -COMPENSATE_CREDIT_PENALTY, order.getId());
        summary.add("补偿：责任方用户 " + defendantId + " 赔付申诉方用户 " + appealerId + " "
                + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " 元"
                + "，责任方信用分 −" + COMPENSATE_CREDIT_PENALTY);
    }

    /** 封号：责任方账号停用 */
    private void doBan(Long defendantId, List<String> summary) {
        if (defendantId == null) {
            throw new BusinessException("无法确定要封禁的用户");
        }
        Users defendant = usersMapper.selectById(defendantId);
        if (defendant == null) {
            throw new BusinessException("责任方用户不存在");
        }
        // 和 AdminController 里「不能禁用管理员账号」保持一致。
        // 教训：管理员登录要求 status=1，把唯一的管理员封掉，管理端就再也登不进来了。
        if (Integer.valueOf(2).equals(defendant.getRole())) {
            throw new BusinessException("不能封禁管理员账号");
        }

        Users patch = new Users();
        patch.setId(defendantId);
        patch.setStatus(0);
        usersMapper.updateById(patch);
        summary.add("封号：已停用责任方用户 " + defendantId
                + (StringUtils.hasText(defendant.getNickname()) ? "（" + defendant.getNickname() + "）" : ""));
    }

    /**
     * 还原提交申诉前的订单状态。
     *
     * appeals 表没有存「申诉前状态」这一列（本次没动表结构），只能从订单自己的时间戳反推：
     * completed_at 非空说明当时已完成(4)，否则退回进行中(2)。
     * 反推不到 3「待确认」的原因：orders 表里没有「送达时间」字段，2 和 3 事后无法区分，
     * 而两者的区别只是跑腿员有没有点过「确认送达」。
     * 退回 2 是保守且安全的 —— 跑腿员重新点一次确认送达即可，不会跳过发布者确认收货那一步，
     * 也就不会绕过资金结算（结算是绑在 4 上的，必须真的确认才发生）。
     */
    private int restoreStatus(Orders order) {
        return order.getCompletedAt() != null ? 4 : 2;
    }

    private void patchOrder(Orders order, int status, int appealStatus) {
        Orders patch = new Orders();
        patch.setId(order.getId());
        patch.setStatus(status);
        patch.setAppealStatus(appealStatus);
        if (status == 4 && order.getCompletedAt() == null) {
            patch.setCompletedAt(LocalDateTime.now());
        }
        if (status == 5 && order.getCancelledAt() == null) {
            patch.setCancelledAt(LocalDateTime.now());
        }
        patch.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(patch);
    }

    /**
     * 从订单反推申诉双方，返回 [申诉方, 被申诉方]；无法确定时返回 null。
     *
     * 为什么不用 appeals 表里存的 appealer_id / defendant_id：campus.sql 的种子数据里
     * 有好几条跟订单对不上（比如申诉 #4 挂在订单 2 上，appealer_id 写的是 2，
     * 而订单 2 的发布者是 3、跑腿员是 6）。仲裁要动钱，双方身份必须以订单为准。
     */
    private Long[] resolveParties(Orders order, Appeals appeal) {
        Long publisher = order.getPublisherId();
        Long runner = order.getRunnerId();
        if (publisher == null || runner == null) {
            return null;
        }

        Long appealer = appeal.getAppealerId();
        if (publisher.equals(appealer)) {
            return new Long[]{publisher, runner};
        }
        if (runner.equals(appealer)) {
            return new Long[]{runner, publisher};
        }

        // appealer_id 对不上，再拿 defendant_id 试一次
        Long defendant = appeal.getDefendantId();
        if (publisher.equals(defendant)) {
            return new Long[]{runner, publisher};
        }
        if (runner.equals(defendant)) {
            return new Long[]{publisher, runner};
        }
        return null;
    }

    /**
     * 扣信用分，下限 0（和 AdminController 里「信用分不能为负」的口径一致）。
     *
     * 原来这里只改 users.credit_score、不写 credit_history（当时 Java 端还没有那张表的实体），
     * 用户看到分数掉了也无从查证。2026-10-15 补上 CreditHistory / CreditScoreService 后，
     * 这里统一收口过去 —— 改分和留痕由 creditScoreService.adjust 一起做。
     */
    private void changeCredit(Long userId, int delta, Long orderId) {
        creditScoreService.adjust(userId, delta, "仲裁判责：补偿申诉方", orderId);
    }

    // ============================ 组装 VO ============================

    private List<AppealVO> toVOs(List<Appeals> appeals) {
        List<AppealVO> result = new ArrayList<>();
        if (appeals == null || appeals.isEmpty()) {
            return result;
        }
        Map<Long, Orders> orderMap = loadOrders(appeals);
        Map<Long, String> nameMap = loadNames(appeals);

        for (Appeals a : appeals) {
            AppealVO vo = new AppealVO();
            vo.setId(a.getId());
            vo.setOrderId(a.getOrderId());
            vo.setAppealerId(a.getAppealerId());
            vo.setDefendantId(a.getDefendantId());
            vo.setAppealerName(nameMap.get(a.getAppealerId()));
            vo.setDefendantName(nameMap.get(a.getDefendantId()));
            vo.setReason(a.getReason());
            vo.setEvidenceUrls(splitEvidence(a.getEvidenceUrls()));
            vo.setStatus(a.getStatus());
            vo.setStatusName(Integer.valueOf(1).equals(a.getStatus()) ? "已处理" : "待处理");
            vo.setAdminRemark(a.getAdminRemark());
            vo.setAdminId(a.getAdminId());
            vo.setAdminName(nameMap.get(a.getAdminId()));
            vo.setCreatedAt(a.getCreatedAt());
            vo.setUpdatedAt(a.getUpdatedAt());

            Orders order = orderMap.get(a.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setTaskType(order.getTaskType());
                vo.setOrderStatus(order.getStatus());
                vo.setOrderStatusName(AppealVO.orderStatusName(order.getStatus()));
                vo.setReward(order.getReward());
            }
            result.add(vo);
        }
        return result;
    }

    private Map<Long, Orders> loadOrders(List<Appeals> appeals) {
        Set<Long> ids = new HashSet<>();
        for (Appeals a : appeals) {
            if (a.getOrderId() != null) {
                ids.add(a.getOrderId());
            }
        }
        Map<Long, Orders> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Orders o : ordersMapper.selectList(new LambdaQueryWrapper<Orders>().in(Orders::getId, ids))) {
            map.put(o.getId(), o);
        }
        return map;
    }

    private Map<Long, String> loadNames(List<Appeals> appeals) {
        Set<Long> ids = new HashSet<>();
        for (Appeals a : appeals) {
            if (a.getAppealerId() != null) {
                ids.add(a.getAppealerId());
            }
            if (a.getDefendantId() != null) {
                ids.add(a.getDefendantId());
            }
            if (a.getAdminId() != null) {
                ids.add(a.getAdminId());
            }
        }
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        // users 表的 deleted 没挂 @TableLogic，必须手写 deleted = 0，否则注销用户也会被查出来
        List<Users> users = usersMapper.selectList(new LambdaQueryWrapper<Users>()
                .in(Users::getId, ids)
                .eq(Users::getDeleted, 0));
        for (Users u : users) {
            map.put(u.getId(), StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getRealName());
        }
        return map;
    }

    /** evidence_urls 是逗号分隔的字符串，拆开并去掉空项 —— 种子里有一条是空串而不是 NULL */
    private List<String> splitEvidence(String raw) {
        List<String> urls = new ArrayList<>();
        if (!StringUtils.hasText(raw)) {
            return urls;
        }
        for (String part : raw.split(",")) {
            if (StringUtils.hasText(part)) {
                urls.add(part.trim());
            }
        }
        return urls;
    }

    /** 证据图只收本系统上传接口产出的路径，挡住外链和 javascript: 这类东西 */
    private List<String> normalizeEvidence(List<String> raw) {
        List<String> urls = new ArrayList<>();
        if (raw == null) {
            return urls;
        }
        for (String url : raw) {
            if (!StringUtils.hasText(url)) {
                continue;
            }
            String trimmed = url.trim();
            if (!trimmed.startsWith("/files/")) {
                throw new BusinessException("证据图片地址不合法，请通过上传接口重新上传");
            }
            urls.add(trimmed);
            if (urls.size() >= MAX_EVIDENCE) {
                break;
            }
        }
        return urls;
    }

    private Set<String> parseActions(List<String> raw) {
        Set<String> actions = new LinkedHashSet<>();
        if (raw == null) {
            return actions;
        }
        for (String action : raw) {
            if (!StringUtils.hasText(action)) {
                continue;
            }
            String value = action.trim().toUpperCase();
            if (!ALL_ACTIONS.contains(value)) {
                throw new BusinessException("不支持的仲裁操作：" + action);
            }
            actions.add(value);
        }
        return actions;
    }

    private boolean contains(String text, String lowerKeyword) {
        return text != null && text.toLowerCase().contains(lowerKeyword);
    }
}
