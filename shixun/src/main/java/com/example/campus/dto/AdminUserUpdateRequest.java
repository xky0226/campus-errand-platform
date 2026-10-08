package com.example.campus.dto;

import lombok.Data;

/**
 * 管理端「用户管理」的编辑 / 禁用启用请求体。
 * <p>
 * 所有字段都可空：MyBatis-Plus 的 updateById 只把非 null 字段拼进 SET，
 * 所以「禁用 / 启用」只传一个 status 就行，其余字段不会被清空。
 * <p>
 * 讲义 3-2 §11 只给了两条管理端用户接口的调用示例
 * （{@code get('/admin/users')} 和 {@code post('/admin/users/verify')}），
 * 没给实现、也没给 UserManage.vue 的字段表。这个 DTO 是照 users 表字段补的。
 */
@Data
public class AdminUserUpdateRequest {

    private String nickname;

    private String realName;

    private String phone;

    /** 0 = 需求者，1 = 跑腿员。管理员(2)不给改，见 AdminController */
    private Integer role;

    private Integer creditScore;

    /** 0 = 禁用，1 = 正常 */
    private Integer status;
}
