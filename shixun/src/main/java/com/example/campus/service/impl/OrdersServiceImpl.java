package com.example.campus.service.impl;

import com.example.campus.dto.AdminOrderRequest;
import com.example.campus.dto.OrderCreateRequest;
import com.example.campus.common.BusinessException;
import com.example.campus.entity.Orders;
import com.example.campus.entity.Users;
import com.example.campus.mapper.OrdersMapper;
import com.example.campus.mapper.UsersMapper;
import com.example.campus.service.IOrdersService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author coder
 * @since 2026-09-21
 */
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements IOrdersService {

    @Autowired
    OrdersMapper ordersMapper;

    @Autowired
    UsersMapper userMapper;

    //    定义一个方法来所有订单根据id查询用户的name信息
    private void fillUserNames(List<Orders> orders) {
        //遍历订单集合
        for (Orders order : orders) {
            //发布者name查询和set
            if (order.getPublisherId() != null) {
                Users publisher = userMapper.selectById(order.getPublisherId());
                if (publisher != null) {
                    order.setPublisherName(publisher.getNickname());
                }
            }
            //跑腿员name查询和set
            if (order.getRunnerId() != null) {
                Users runner = userMapper.selectById(order.getRunnerId());
                if (runner != null) {
                    order.setRunnerName(runner.getNickname());
                }
            }
        }
    }

    @Override
    public List<Orders> listOrders() {
        //构建条件构造器对象
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //设置条件
        queryWrapper.eq(Orders::getDeleted,0);//没有删除的状态
        queryWrapper.orderByDesc(Orders::getCreatedAt);//根据时间做降序排序
        //查询
        List<Orders> orders = ordersMapper.selectList(queryWrapper);
        //关联查询设置发布者和跑腿员的name
        fillUserNames(orders);
        return orders;
    }

    @Override
    public Orders getOrderDetail(Long orderId) {
        //根据主键id查询
        Orders order = ordersMapper.selectById(orderId);
        //没有查询到数据则则抛出异常处理
        if (order == null || order.getDeleted() != 0) {
            throw new BusinessException("订单不存在");
        }
        //发布者name查询和set
        if (order.getPublisherId() != null) {
            Users publisher = userMapper.selectById(order.getPublisherId());
            if (publisher != null) {
                order.setPublisherName(publisher.getNickname());
            }
        }
        //跑腿员name查询和set
        if (order.getRunnerId() != null) {
            Users runner = userMapper.selectById(order.getRunnerId());
            if (runner != null) {
                order.setRunnerName(runner.getNickname());
            }
        }
        return order;
    }

    @Override
    public Orders createOrder(AdminOrderRequest request) {
        //请求参数 -> 实体
        Orders order = new Orders();
        order.setPublisherId(request.getPublisherId());
        order.setTaskType(request.getTaskType());
        order.setReward(request.getReward());
        order.setPickupLocation(request.getPickupLocation());
        order.setPickupLatitude(request.getPickupLatitude());
        order.setPickupLongitude(request.getPickupLongitude());
        order.setDeliveryLocation(request.getDeliveryLocation());
        order.setDeliveryLatitude(request.getDeliveryLatitude());
        order.setDeliveryLongitude(request.getDeliveryLongitude());
        order.setExpectedTime(request.getExpectedTime());
        order.setDescription(request.getDescription());
        //生成订单号：CH + yyyyMMddHHmmss + 3位随机数
        order.setOrderNo("CH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 1000));
        //status、payment_status、platform_fee 等字段这里不赋值，
        //MyBatis Plus 默认只把不为 null 的字段写进 INSERT，交给数据库默认值处理
        ordersMapper.insert(order);
        //新增后 MP 会把自增主键回填到 order.id
        return order;
    }

    @Override
    public Boolean updateOrder(Long orderId, AdminOrderRequest request) {
        //先查出现有订单，用于业务校验
        Orders exist = ordersMapper.selectById(orderId);
        if (exist == null) {
            throw new RuntimeException("订单不存在");
        }
        //"未开始执行"的订单才能编辑：0待支付、1待接单
        if (exist.getStatus() != 0 && exist.getStatus() != 1) {
            throw new RuntimeException("订单已开始执行，不能编辑");
        }
        //请求参数 -> 实体，只带需要改的字段（updateById 只更新不为 null 的字段）
        Orders order = new Orders();
        order.setId(orderId);
        order.setTaskType(request.getTaskType());
        order.setReward(request.getReward());
        order.setPickupLocation(request.getPickupLocation());
        order.setPickupLatitude(request.getPickupLatitude());
        order.setPickupLongitude(request.getPickupLongitude());
        order.setDeliveryLocation(request.getDeliveryLocation());
        order.setDeliveryLatitude(request.getDeliveryLatitude());
        order.setDeliveryLongitude(request.getDeliveryLongitude());
        order.setExpectedTime(request.getExpectedTime());
        order.setDescription(request.getDescription());
        return ordersMapper.updateById(order) > 0;
    }

    @Override
    public Boolean forceCancelOrder(Long orderId) {
        Orders exist = ordersMapper.selectById(orderId);
        if (exist == null) {
            throw new RuntimeException("订单不存在");
        }
        //"未完成"的订单才能强制取消：0待支付、1待接单、2进行中、3待确认、6申诉中
        //已完成(4)、已取消(5) 不允许
        List<Integer> unfinished = Arrays.asList(0, 1, 2, 3, 6);
        if (!unfinished.contains(exist.getStatus())) {
            throw new RuntimeException("订单当前状态不能强制取消");
        }
        Orders order = new Orders();
        order.setId(orderId);
        order.setStatus(5);//5 已取消
        order.setCancelledAt(LocalDateTime.now());
        return ordersMapper.updateById(order) > 0;
    }

    @Override
    public Boolean deleteOrder(Long orderId) {
        Orders exist = ordersMapper.selectById(orderId);
        if (exist == null) {
            throw new RuntimeException("订单不存在");
        }
        //@TableLogic：deleteById 会自动变成 UPDATE orders SET deleted=1 WHERE id=? AND deleted=0
        return ordersMapper.deleteById(orderId) > 0;
    }

    //=================== 以下为讲义「4、移动端-构建」第 12.1 节新增的小程序端接口 ===================

    //用户发布订单
    @Override
    @Transactional
    public Orders createClientOrder(Long publisherId, OrderCreateRequest request) {
        Users publisher = userMapper.selectById(publisherId);
        if (publisher == null) throw new BusinessException("用户不存在");
        //可以添加信用分的逻辑判断
//        if (!creditService.canPublishOrder(publisherId)) {
//            throw new BusinessException("信用分过低，无法发布订单");
//        }

        BigDecimal reward = request.getReward();
        if (reward == null || reward.compareTo(BigDecimal.valueOf(2)) < 0) {
            throw new BusinessException("订单金额不能低于2元");
        }

        Orders order = new Orders();
        //用户端订单号前缀。讲义 (1) 原文是 "ADMIN"，(2) 改成了 "Client"
        //（管理员那份 createOrder 用的是 "CH"+时间+随机数）
        order.setOrderNo("Client" + System.currentTimeMillis());
        order.setPublisherId(publisherId);
        order.setTaskType(request.getTaskType());
        order.setPickupLocation(request.getPickupLocation());
        order.setDeliveryLocation(request.getDeliveryLocation());
        order.setPickupLatitude(request.getPickupLatitude());
        order.setPickupLongitude(request.getPickupLongitude());
        order.setDeliveryLatitude(request.getDeliveryLatitude());
        order.setDeliveryLongitude(request.getDeliveryLongitude());
        order.setDescription(request.getDescription());
        order.setImageUrls(request.getImageUrls());
        order.setExpectedTime(request.getExpectedTime() != null ? request.getExpectedTime() : LocalDateTime.now().plusHours(24));
        order.setReward(reward);
        order.setStatus(0);
        order.setPaymentStatus(0);
        order.setPlatformFee(BigDecimal.ZERO);
        order.setRunnerEarnings(reward.subtract(order.getPlatformFee()));
        ordersMapper.insert(order);
        return order;
    }

    //可接订单（status=1 待接单）
    @Override
    public List<Orders> getAvailableOrders() {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getDeleted, 0)
                .eq(Orders::getStatus, 1)
                .orderByDesc(Orders::getCreatedAt);
        List<Orders> orders = ordersMapper.selectList(wrapper);
        fillUserNames(orders);
        return orders;
    }

    //我发布的订单
    @Override
    public List<Orders> getPublisherOrders(Long publisherId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getDeleted, 0)
                .eq(Orders::getPublisherId, publisherId)
                .orderByDesc(Orders::getCreatedAt);
        List<Orders> orders = ordersMapper.selectList(wrapper);
        fillUserNames(orders);
        return orders;
    }

    //我接取的订单
    @Override
    public List<Orders> getRunnerOrders(Long runnerId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getDeleted, 0)
                .eq(Orders::getRunnerId, runnerId)
                .orderByDesc(Orders::getCreatedAt);
        List<Orders> orders = ordersMapper.selectList(wrapper);
        fillUserNames(orders);
        return orders;
    }

    @Override
    public Orders getOrderById(Long orderId) {
        return getOrderDetail(orderId);
    }

    //支付订单
    @Override
    @Transactional
    public Orders payOrder(Long userId, Long orderId, String payMethod) {
        Orders order = getOrderForUpdate(orderId);
        if (!userId.equals(order.getPublisherId())) {
            throw new BusinessException("只能支付自己发布的订单");
        }
        if (!Integer.valueOf(0).equals(order.getStatus())) {
            throw new BusinessException("订单当前状态不可支付");
        }

        order.setPaymentStatus(1);
        order.setStatus(1);
        //pay_time 在 DDL 里是 varchar(50)，所以这里用字符串存
        order.setPayTime(LocalDateTime.now().toString());
        order.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        return getOrderDetail(orderId);
    }

    //接单
    @Override
    @Transactional
    public Orders acceptOrder(Long runnerId, Long orderId) {
        Orders order = getOrderForUpdate(orderId);
        if (runnerId.equals(order.getPublisherId())) {
            throw new BusinessException("不能接取自己发布的订单");
        }
        if (!Integer.valueOf(1).equals(order.getStatus())) {
            throw new BusinessException("订单当前状态不可接单");
        }

        order.setRunnerId(runnerId);
        order.setStatus(2);
        order.setAcceptedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        return getOrderDetail(orderId);
    }

    //跑腿员确认送达
    @Override
    @Transactional
    public Orders confirmDelivery(Long runnerId, Long orderId) {
        Orders order = getOrderForUpdate(orderId);
        if (!runnerId.equals(order.getRunnerId())) {
            throw new BusinessException("只能操作自己接取的订单");
        }
        if (!Integer.valueOf(2).equals(order.getStatus())) {
            throw new BusinessException("订单当前状态不可确认送达");
        }

        order.setStatus(3);
        order.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        return getOrderDetail(orderId);
    }

    //发布者确认收货
    @Override
    @Transactional
    public Orders confirmReceipt(Long publisherId, Long orderId) {
        Orders order = getOrderForUpdate(orderId);
        if (!publisherId.equals(order.getPublisherId())) {
            throw new BusinessException("只能确认自己发布的订单");
        }
        if (!Integer.valueOf(3).equals(order.getStatus())) {
            throw new BusinessException("订单当前状态不可确认完成");
        }

        order.setStatus(4);
        order.setCompletedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        return getOrderDetail(orderId);
    }

    //取消订单
    @Override
    @Transactional
    public Orders cancelOrder(Long userId, Long orderId, String reason) {
        Orders order = getOrderForUpdate(orderId);
        boolean isPublisher = userId.equals(order.getPublisherId());
        boolean isRunner = order.getRunnerId() != null && userId.equals(order.getRunnerId());
        if (!isPublisher && !isRunner) {
            throw new BusinessException("只能取消自己相关的订单");
        }
        if (order.getStatus() == 4) {
            throw new BusinessException("已完成订单不能取消");
        }
        if (order.getStatus() == 5) {
            return getOrderDetail(orderId);
        }

        //reason 目前只透传没落库：orders 表没有取消原因字段
        order.setStatus(5);
        order.setCancelledAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        return getOrderDetail(orderId);
    }

    //用户编辑订单，只允许改未开始执行的订单
    @Override
    @Transactional
    public Orders updateOrder(Long userId, Long orderId, OrderCreateRequest request) {
        Orders order = getOrderForUpdate(orderId);
        if (!userId.equals(order.getPublisherId())) {
            throw new BusinessException("只能修改自己发布的订单");
        }
        if (!Integer.valueOf(0).equals(order.getStatus()) && !Integer.valueOf(1).equals(order.getStatus())) {
            throw new BusinessException("订单已开始执行，无法修改");
        }

        order.setTaskType(request.getTaskType());
        order.setReward(request.getReward());
        order.setPickupLocation(request.getPickupLocation());
        order.setPickupLatitude(request.getPickupLatitude());
        order.setPickupLongitude(request.getPickupLongitude());
        order.setDeliveryLocation(request.getDeliveryLocation());
        order.setDeliveryLatitude(request.getDeliveryLatitude());
        order.setDeliveryLongitude(request.getDeliveryLongitude());
        order.setExpectedTime(request.getExpectedTime());
        order.setDescription(request.getDescription());
        order.setImageUrls(request.getImageUrls());
        order.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
        return getOrderDetail(orderId);
    }

    //评价订单
    @Override
    @Transactional
    public void rateOrder(Long userId, Long orderId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评分必须在1到5之间");
        }
        Orders order = getOrderForUpdate(orderId);
        if (!Integer.valueOf(4).equals(order.getStatus())) {
            throw new BusinessException("订单完成后才能评价");
        }

        if (userId.equals(order.getPublisherId())) {
            if (order.getPublisherRated() != null && order.getPublisherRated() == 1) {
                throw new BusinessException("发布者已评价");
            }
            order.setPublisherRated(1);
            order.setRunnerRating(rating);
            order.setPublisherComment(comment);
        } else if (userId.equals(order.getRunnerId())) {
            if (order.getRunnerRated() != null && order.getRunnerRated() == 1) {
                throw new BusinessException("跑腿员已评价");
            }
            order.setRunnerRated(1);
            order.setPublisherRating(rating);
            order.setRunnerComment(comment);
        } else {
            throw new BusinessException("只能评价自己相关的订单");
        }

        order.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(order);
    }

    //取出订单并做存在性校验，给需要改状态的方法共用
    private Orders getOrderForUpdate(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || order.getDeleted() != 0) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }
}
