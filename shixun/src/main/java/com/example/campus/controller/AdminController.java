package com.example.campus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.campus.common.BusinessException;
import com.example.campus.common.VerifyStatus;
import com.example.campus.dto.AdminUserUpdateRequest;
import com.example.campus.dto.CreditAdjustRequest;
import com.example.campus.dto.Result;
import com.example.campus.dto.VerifyAuditRequest;
import com.example.campus.entity.CreditHistory;
import com.example.campus.entity.Users;
import com.example.campus.mapper.UsersMapper;
import com.example.campus.service.ICreditScoreService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 - 用户管理。
 * <p>
 * 讲义 3-2 §8 原先只有个 role 校验的 demo：{@code GET /admin/users} 直接
 * {@code return Result.success(role)}，把数字 2 当结果返回，并不查库。
 * 讲义 §11「页面请求示例」里却又写着 {@code const users = await get('/admin/users')}
 * 当作「查询用户列表」用 —— 前后不一致。这里按 §11 的语义把它改成真的用户列表，
 * 讲义那个 demo 就不再保留了。
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsersMapper usersMapper;

    private final ICreditScoreService creditScoreService;

    /**
     * 用户列表。一次返回全部（前端做搜索 / 筛选 / 分页，和订单管理页一致）。
     * users 表没有挂 @TableLogic，所以 deleted 要自己过滤。
     */
    @GetMapping("/users")
    public Result<List<Users>> listUsers(HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getDeleted, 0).orderByDesc(Users::getId);
        return Result.success(usersMapper.selectList(wrapper));
    }

    /**
     * 编辑用户资料 / 禁用启用。
     * <p>
     * 路径是补的：讲义只给了 {@code GET /admin/users} 和
     * {@code POST /admin/users/verify}（后者是认证审核，本轮没做）。
     */
    @PutMapping("/users/{id}")
    public Result<Users> updateUser(@PathVariable Long id,
                                    @RequestBody AdminUserUpdateRequest req,
                                    HttpServletRequest request) {
        requireAdmin(request);

        Users user = usersMapper.selectById(id);
        if (user == null || user.getDeleted() != 0) {
            throw new BusinessException("用户不存在");
        }

        // 只把传了的字段放进 patch，没传的字段 MP 不会动
        Users patch = new Users();
        patch.setId(id);
        boolean hasChange = false;

        if (req.getNickname() != null) {
            String nickname = req.getNickname().trim();
            if (nickname.isEmpty()) {
                throw new BusinessException("昵称不能为空");
            }
            if (nickname.length() > 64) {
                throw new BusinessException("昵称不能超过 64 个字");
            }
            patch.setNickname(nickname);
            hasChange = true;
        }

        if (req.getRealName() != null) {
            String realName = req.getRealName().trim();
            if (realName.length() > 32) {
                throw new BusinessException("真实姓名不能超过 32 个字");
            }
            patch.setRealName(realName);
            hasChange = true;
        }

        if (req.getPhone() != null) {
            String phone = req.getPhone().trim();
            if (phone.length() > 16) {
                throw new BusinessException("手机号不能超过 16 位");
            }
            patch.setPhone(phone);
            hasChange = true;
        }

        if (req.getRole() != null) {
            // role=2 是管理员，管理端不给改，免得把普通用户提成管理员
            if (req.getRole() != 0 && req.getRole() != 1) {
                throw new BusinessException("角色只能是 0(需求者) 或 1(跑腿员)");
            }
            patch.setRole(req.getRole());
            hasChange = true;
        }

        if (req.getCreditScore() != null) {
            // 库里现有数据信用分范围是 45~102，默认 100，没有明确上限，所以只挡负数
            if (req.getCreditScore() < 0) {
                throw new BusinessException("信用分不能为负数");
            }
            patch.setCreditScore(req.getCreditScore());
            hasChange = true;
        }

        if (req.getStatus() != null) {
            if (req.getStatus() != 0 && req.getStatus() != 1) {
                throw new BusinessException("状态只能是 0(禁用) 或 1(正常)");
            }
            // adminLogin / 用户端 login 都判了 status != 1 就拒绝登录，
            // 一旦把唯一的管理员禁掉就再也登不进管理端了，所以这里挡住
            if (req.getStatus() == 0 && user.getRole() != null && user.getRole() == 2) {
                throw new BusinessException("不能禁用管理员账号");
            }
            patch.setStatus(req.getStatus());
            hasChange = true;
        }

        if (!hasChange) {
            throw new BusinessException("没有要修改的内容");
        }

        usersMapper.updateById(patch);
        return Result.success(usersMapper.selectById(id));
    }

    // ================= 实名认证审核（需求 3.5.1 剩下的部分） =================
    // 三个路径都是照 permissions 表写死的，不是自己编的：
    //   admin:verify:pending → /admin/users/pending-verify
    //   admin:user:credit   → /admin/users/*/credit
    // 审核动作本身 permissions 里没有对应行，按 REST 习惯补成 POST /admin/users/{id}/verify
    // （讲义 3-2 §11 的调用示例里出现过 post('/admin/users/verify')，但没给实现）。

    /** 待审核的实名认证列表：verified_status = 2（审核中） */
    @GetMapping("/users/pending-verify")
    public Result<List<Users>> pendingVerify(HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getDeleted, 0)
                .eq(Users::getVerifiedStatus, VerifyStatus.PENDING)
                // 按提交先后排，先提交的先审
                .orderByAsc(Users::getUpdatedAt)
                .orderByAsc(Users::getId);
        return Result.success(usersMapper.selectList(wrapper));
    }

    /**
     * 实名认证审核：通过 → verified_status=1；驳回 → 0，并清掉学生证图。
     * <p>
     * 只有当前是「审核中(2)」的才能审。已经认证过的再点通过会重复计入，
     * 已驳回的也不该被直接改成通过（否则管理员的一次误点就绕过了本人重新提交）。
     */
    @PostMapping("/users/{id}/verify")
    public Result<Users> verify(@PathVariable Long id,
                                @RequestBody VerifyAuditRequest req,
                                HttpServletRequest request) {
        requireAdmin(request);
        if (req.getPass() == null) {
            throw new BusinessException("请指定审核结果 pass=true/false");
        }

        Users user = usersMapper.selectById(id);
        if (user == null || !Integer.valueOf(0).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        if (!Integer.valueOf(VerifyStatus.PENDING).equals(user.getVerifiedStatus())) {
            throw new BusinessException("该用户当前是「" + VerifyStatus.name(user.getVerifiedStatus())
                    + "」，不是待审核状态");
        }

        if (req.getPass()) {
            usersMapper.update(null, Wrappers.<Users>lambdaUpdate()
                    .set(Users::getVerifiedStatus, VerifyStatus.PASSED)
                    .eq(Users::getId, id));
        } else {
            String reason = req.getReason() == null ? "" : req.getReason().trim();
            if (!StringUtils.hasText(reason)) {
                // 驳回必须说明理由，否则用户被驳回后完全不知道自己哪里不对
                throw new BusinessException("驳回时请填写理由");
            }
            // 学生证图要一起清掉：这张图已经被判定不合格，留着会让下次审核
            // 误以为是新提交的图。顺带逼用户重新上传。
            // 注意这里不能用 updateById，MyBatis-Plus 默认忽略 null 字段，set 不掉。
            usersMapper.update(null, Wrappers.<Users>lambdaUpdate()
                    .set(Users::getVerifiedStatus, VerifyStatus.NONE)
                    .set(Users::getStudentCertUrl, null)
                    .eq(Users::getId, id));
            // 目前没地方存驳回理由（users 表没有这一列，本次不动表结构），
            // 只能记日志 + 回给管理端。用户端看不到 —— 这是已知缺口。
            log.info("管理员 {} 驳回用户 {} 的实名认证，理由：{}", request.getAttribute("userId"), id, reason);
        }

        return Result.success(usersMapper.selectById(id));
    }

    /** 某个用户的信用分变动历史（管理端「信用分历史」弹窗，新的在前） */
    @GetMapping("/users/{id}/credit-history")
    public Result<List<CreditHistory>> creditHistory(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        Users user = usersMapper.selectById(id);
        if (user == null || !Integer.valueOf(0).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        return Result.success(creditScoreService.history(id));
    }

    /**
     * 手动调整信用分。走 ICreditScoreService，改分和写 credit_history 一起完成。
     * <p>
     * 和 {@code PUT /admin/users/{id}} 传 creditScore 的区别：那个是「直接把分数设成某个值」，
     * 只改数不留痕；这个传的是「变动值」，会留一条可追溯的历史记录。
     * 两者并存，管理端「信用分历史」页用的是这个。
     */
    @PostMapping("/users/{id}/credit")
    public Result<CreditHistory> adjustCredit(@PathVariable Long id,
                                              @RequestBody CreditAdjustRequest req,
                                              HttpServletRequest request) {
        requireAdmin(request);
        if (req.getDelta() == null || req.getDelta() == 0) {
            throw new BusinessException("请填写不为 0 的信用分变动值");
        }

        CreditHistory history = creditScoreService.adjust(id, req.getDelta(), req.getReason(), null);
        if (history == null) {
            // adjust 返回 null 只有一种情况：分数已经是 0 还在往下扣，实际没有变动
            throw new BusinessException("信用分没有变化（该用户当前已是 0 分）");
        }
        return Result.success(history);
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
