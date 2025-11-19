package com.sky.mapper;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import java.util.List;

@Mapper
public interface OrderMapper {

    void add(Orders orders);

    void addOrderDetail(List<OrderDetail> orderDetails);
}
