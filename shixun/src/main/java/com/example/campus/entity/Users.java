package com.example.campus.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author coder
 * @since 2026-09-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Users implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String openId;

    private String nickname;

    private String studentId;

    private Integer role;

    private String avatarUrl;

    private String realName;

    private String phone;

    private Integer verifiedStatus;

    private Integer creditScore;

    private Integer status;

    private BigDecimal walletBalance;

    private BigDecimal frozenBalance;

    private BigDecimal totalEarnings;

    private Integer completedOrders;

    private BigDecimal acceptanceRate;

    private String studentCertUrl;

    private String alipayAccount;

    private String latitude;

    private String longitude;

    private Integer isOnline;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer deleted;

    // BCrypt 密文，不要返回给前端（登录响应里带的是整个 Users 对象，
    // /user/query 那些接口也返回 Users）
    @JsonIgnore
    private String password;


}
