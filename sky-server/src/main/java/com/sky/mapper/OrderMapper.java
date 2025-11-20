package com.sky.mapper;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    void add(Orders orders);

    void addOrderDetail(List<OrderDetail> orderDetails);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);
    //订单数量
    Long historyOrdersCount(OrdersPageQueryDTO ordersPageQueryDTO);
    //订单列表
    List<Orders> historyOrders(OrdersPageQueryDTO ordersPageQueryDTO);
    //订单详情
    List<OrderDetail> getOrderDetail(Long id);
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    Long allHistoryOrdersCount(OrdersPageQueryDTO ordersPageQueryDTO);

    List<OrderVO> allHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO);
    @Select("select sum(status=2) toBeConfirmed,sum(status=3) confirmed,sum(status=4) deliveryInProgress from orders;")
    OrderStatisticsVO statistics();
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndTimeout(@Param("status") Integer pendingPayment,
                                       @Param("orderTime") LocalDateTime orderTime);
}
