package com.example.campus.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 订单表
 * </p>
 *
 * @author coder
 * @since 2026-09-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long publisherId;

    private Long runnerId;

    private String taskType;

    private String pickupLocation;

    private String deliveryLocation;

    private String pickupLatitude;

    private String pickupLongitude;

    private String deliveryLatitude;

    private String deliveryLongitude;

    private String description;

    private String imageUrls;

    private LocalDateTime expectedTime;

    private BigDecimal reward;

    /**
     * 0: 待支付, 1: 待接单, 2: 进行中, 3: 待确认, 4: 已完成, 5: 已取消, 6: 申诉中
     */
    private Integer status;

    private Integer paymentStatus;

    private String alipayTradeNo;

    /**
     * 支付时间
     */
    private String payTime;

    private BigDecimal platformFee;

    private BigDecimal runnerEarnings;

    private LocalDateTime acceptedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private Integer publisherRated;

    private Integer runnerRated;

    private Integer publisherRating;

    private Integer runnerRating;

    private String publisherComment;

    private String runnerComment;

    private Integer appealStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记：0 未删除，1 已删除
     * 加了 @TableLogic 之后，deleteById 会自动变成 UPDATE ... SET deleted=1
     */
    @TableLogic
    private Integer deleted;

    /**
     * 跑腿员上传的凭证图片URL，多张用逗号分隔
     */
    private String proofImageUrls;

    //    关联依赖变量
    @TableField(exist = false)
    private String publisherName;
    @TableField(exist = false)
    private String runnerName;

}
