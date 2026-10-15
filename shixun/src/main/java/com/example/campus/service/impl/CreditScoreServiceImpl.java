package com.example.campus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.campus.common.BusinessException;
import com.example.campus.entity.CreditHistory;
import com.example.campus.entity.Users;
import com.example.campus.mapper.CreditHistoryMapper;
import com.example.campus.mapper.UsersMapper;
import com.example.campus.service.ICreditScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 信用分服务实现。
 *
 * @author coder
 * @since 2026-10-15
 */
@Slf4j
@Service
public class CreditScoreServiceImpl implements ICreditScoreService {

    /** users.credit_score 没写默认值，注册时是显式设的 100，这里兜底用同一个数 */
    private static final int DEFAULT_SCORE = 100;

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private CreditHistoryMapper creditHistoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditHistory adjust(Long userId, int delta, String reason, Long relatedOrderId) {
        if (userId == null) {
            throw new BusinessException("缺少用户ID");
        }
        if (delta == 0) {
            // 0 分变动没有意义，不写一条 delta=0 的空记录
            return null;
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("请填写信用分变动原因");
        }

        // Users 没挂 @TableLogic，deleted 要自己判
        Users user = usersMapper.selectById(userId);
        if (user == null || !Integer.valueOf(0).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }

        int before = user.getCreditScore() == null ? DEFAULT_SCORE : user.getCreditScore();
        // 信用分最低 0，不给负数。扣到 0 就停住
        int after = Math.max(0, before + delta);
        if (after == before) {
            // 已经是 0 分还继续扣，实际没变化 —— 返回 null，让调用方知道「没发生什么」，
            // 免得写入一条 delta=-5 但 before/after 都是 0 的自相矛盾记录
            log.info("用户 {} 信用分已是 {}，扣分 {} 无效，跳过", userId, before, delta);
            return null;
        }

        Users patch = new Users();
        patch.setId(userId);
        patch.setCreditScore(after);
        usersMapper.updateById(patch);

        CreditHistory history = new CreditHistory();
        history.setUserId(userId);
        // 存「实际变动」而不是传进来的 delta：
        // 99 分扣 5 记 -5，但 3 分扣 5 实际只掉了 3 分，记 -3 才能和 before/after 对上。
        // 否则前端把 delta 加起来会发现和分数变化对不上账。
        history.setDelta(after - before);
        history.setBeforeScore(before);
        history.setAfterScore(after);
        history.setReason(reason.trim());
        history.setRelatedOrderId(relatedOrderId);
        // 显式给时间：表里有 DEFAULT CURRENT_TIMESTAMP，但这样 insert 完不用再查一次就有完整对象可返回
        history.setCreatedAt(LocalDateTime.now());
        creditHistoryMapper.insert(history);

        log.info("用户 {} 信用分 {} -> {}（{}），原因：{}", userId, before, after, after - before, reason);
        return history;
    }

    @Override
    public List<CreditHistory> history(Long userId) {
        if (userId == null) {
            throw new BusinessException("缺少用户ID");
        }
        // created_at 相同的记录（同一秒内连着改两次）按 id 倒序兜底，保证顺序稳定
        return creditHistoryMapper.selectList(new LambdaQueryWrapper<CreditHistory>()
                .eq(CreditHistory::getUserId, userId)
                .orderByDesc(CreditHistory::getCreatedAt)
                .orderByDesc(CreditHistory::getId));
    }
}
