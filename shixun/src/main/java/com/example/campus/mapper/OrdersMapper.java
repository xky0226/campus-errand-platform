package com.example.campus.mapper;

import com.example.campus.dto.AdminOrderTrendItem;
import com.example.campus.entity.Orders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 订单表 Mapper 接口
 * </p>
 *
 * @author coder
 * @since 2026-09-21
 */
public interface OrdersMapper extends BaseMapper<Orders> {

    //统计已经支付的订单数量
    @Select("SELECT COALESCE(SUM(reward), 0) FROM `orders` WHERE payment_status = 1 AND deleted = 0")
    BigDecimal sumPaidOrderAmount();

    //查询每天的订单数量
    @Select("SELECT DATE(created_at) AS date, COUNT(*) AS count " +
            "FROM `orders` " +
            "WHERE created_at >= #{startDate} AND deleted = 0 " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY DATE(created_at)")
    List<AdminOrderTrendItem> countOrdersByDate(@Param("startDate") LocalDate startDate);

}
