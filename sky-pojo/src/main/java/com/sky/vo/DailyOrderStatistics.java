package com.sky.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyOrderStatistics {

    // 对应 SQL: DATE(order_time) AS orderDate
    private LocalDate orderDate;

    // 每天总订单数
    private Integer totalCount;

    // 每天有效订单数（status = 5）
    private Integer validCount;
}
