package com.example.campus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 申诉仲裁表
 * </p>
 *
 * @author coder
 * @since 2026-09-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Appeals implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long appealerId;

    private Long defendantId;

    private String reason;

    private String evidenceUrls;

    private Integer status;

    private String adminRemark;

    private Long adminId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
