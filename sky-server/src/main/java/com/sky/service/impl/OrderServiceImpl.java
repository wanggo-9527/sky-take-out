package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

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
        AddressBook addressBook = addressBookMapper.getById(ordersDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException("地址不存在，点个蛋的外卖");
        }
        checkOutOfRange ( addressBook.getCityName () + addressBook.getDistrictName () + addressBook.getDetail () );
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
        //orderMapper.add(orders);
        // ⭐⭐ 关键：把地址信息写入订单表 ⭐⭐
        orders.setConsignee(addressBook.getConsignee());  // 收货人
        orders.setPhone(addressBook.getPhone());         // 电话
        orders.setAddress(
                (addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName()) +
                        (addressBook.getCityName() == null ? "" : addressBook.getCityName()) +
                        (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName()) +
                        (addressBook.getDetail() == null ? "" : addressBook.getDetail())
        );
        orders.setUserName(addressBook.getConsignee());  // 前端也会用这个

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

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 1. 取出订单号
        String orderNumber = ordersPaymentDTO.getOrderNumber();
        log.info("【模拟支付】订单号：{}", orderNumber);

        // 2. 根据订单号查询订单，防止乱写单号
        Orders ordersDB = orderMapper.getByNumber(orderNumber);
        if (ordersDB == null) {
            log.error("【模拟支付】订单不存在：{}", orderNumber);
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 3. 直接当付款成功处理
        //    原来是等微信回调里调用 paySuccess，现在我们自己调
        this.paySuccess(orderNumber);

        // 4. 返回一个假的支付参数，前端如果只是拿到 result 就当成功，根本不会管内容
        OrderPaymentVO vo = OrderPaymentVO.builder()
                .nonceStr("mock_nonce")
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .signType("MOCK")
                .packageStr("MOCK_PACKAGE")
                .paySign("MOCK_SIGN")
                .build();

        log.info("【模拟支付】订单 {} 已模拟支付成功", orderNumber);
        return vo;
    }


    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "用户：" + ordersDB.getUserName() + "，订单：" + ordersDB.getNumber() + "，支付成功！");
        webSocketServer.sendToAllClient(JSON.toJSONString(map));

    }

    @Override
    public PageResult historyOrders(Integer page, Integer pageSize, Integer status) {
        page=(page-1)*pageSize;//计算当前页的起始位置
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();//封装查询参数
        ordersPageQueryDTO.setStatus(status);//订单状态
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());//用户id
        ordersPageQueryDTO.setPage(page);//当前页
        ordersPageQueryDTO.setPageSize(pageSize);//每页显示数量
        Long total = orderMapper.historyOrdersCount(ordersPageQueryDTO);//订单总数
        List<Orders> orders = orderMapper.historyOrders(ordersPageQueryDTO);//订单
        List<OrderVO> orderVOList = new ArrayList<>();//订单详情
        if(orders != null&&orders.size()>0){//订单不为空
            for (Orders order : orders) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);

                // 查询订单详情列表
                List<OrderDetail> detailList = orderMapper.getOrderDetail(order.getId());
                orderVO.setOrderDetailList(detailList);

                // 拼接 orderDishes，比如 “汉堡x1 可乐x2”
                StringBuilder sb = new StringBuilder();
                for (OrderDetail od : detailList) {
                    sb.append(od.getName())
                            .append("x")
                            .append(od.getNumber())
                            .append(" ");
                }
                orderVO.setOrderDishes(sb.toString().trim());

                orderVOList.add(orderVO);
            }

        }

        return new PageResult(total,orderVOList);

    }

    @Override
    public OrderVO orderDetail(Long id) {

        OrderVO orderVO = new OrderVO();
        Orders orders = orderMapper.getById(id);
        BeanUtils.copyProperties(orders,orderVO);
        List<OrderDetail> orderDetailList = orderMapper.getOrderDetail(id);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    ordersDB.getNumber(), //商户订单号
                    ordersDB.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        ordersPageQueryDTO.setPage((ordersPageQueryDTO.getPage()-1)*ordersPageQueryDTO.getPageSize());
        Long total = orderMapper.allHistoryOrdersCount(ordersPageQueryDTO);
        List<OrderVO> orderVOList = orderMapper.allHistoryOrders(ordersPageQueryDTO);
        return new PageResult(total,orderVOList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        return orderMapper.statistics();
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        orderMapper.update(Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(3)
                .build());
    }

    @Override
    public String rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        if(orders.getStatus()==2){
        orderMapper.update(Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(6)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build());
                return null;}
        else return "早干嘛去了现在拒单";
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }
    @Override
    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        // 根据id查询订单
        Orders order= orderMapper.getById(id);
        if(order.getStatus()!=3){
            throw new OrderBusinessException("订单状态错误");
        }
        orderMapper.update(Orders.builder()
                .id(id)
                .status(4)
                .deliveryTime(LocalDateTime.now())
                .build());
    }

    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    @Override
    public void reminder(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Map<String,Object> map = new HashMap();
        map.put("type","2");
        map.put("orderId",id);
        map.put("content","订单："+  id+"提醒成功");

        webSocketServer.sendToAllClient("订单："+id+"提醒成功");
    }


    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
