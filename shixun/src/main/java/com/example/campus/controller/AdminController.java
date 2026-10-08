package com.example.campus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.campus.common.BusinessException;
import com.example.campus.dto.AdminUserUpdateRequest;
import com.example.campus.dto.Result;
import com.example.campus.entity.Users;
import com.example.campus.mapper.UsersMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsersMapper usersMapper;

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

    /** 管理端接口统一走这个：JWT 拦截器已经把 role 塞进 request 了 */
    private void requireAdmin(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        //管理员角色role为2、0为需求者、1为跑腿员
        if (role == null || role != 2) {
            throw new BusinessException("无管理员权限");
        }
    }
}
