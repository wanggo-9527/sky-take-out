package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.OrdersDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Transactional
    @Override
    public OrderSubmitVO submit(OrdersDTO ordersDTO) {

        List<ShoppingCart> carts =shoppingCartMapper.list(BaseContext.getCurrentId());
        //1 空地址 空购物车
        if(ordersDTO.getAddressBookId() == null) {
            throw new AddressBookBusinessException("不填地址点个蛋的外卖");
        }
        if (carts == null || carts.size() == 0) {
            throw new AddressBookBusinessException("没钱点个蛋的外卖");
        }
        //2 插入订单
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersDTO, orders);
        orders.setUserId(BaseContext.getCurrentId());
        orders.setStatus(1);
        orders.setPayStatus(0);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setPayMethod(1);
        orderMapper.add(orders);

        //3 插入订单详情表
        List<OrderDetail> orderDetails = new ArrayList<>();
        for(ShoppingCart cart : carts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);

            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);

        }
        orderMapper.addOrderDetail(orderDetails);
        //清空购物车
        shoppingCartMapper.clean(BaseContext.getCurrentId());

        //5 返回
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }
}
