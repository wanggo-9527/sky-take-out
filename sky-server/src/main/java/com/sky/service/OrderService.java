package com.sky.service;

import com.sky.dto.OrdersDTO;
import com.sky.vo.OrderSubmitVO;

public interface OrderService {
    OrderSubmitVO submit(OrdersDTO ordersDTO);
}
