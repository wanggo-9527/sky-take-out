package com.sky.mapper;

import com.sky.vo.DailyOrderStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {
    Double turnoverStatistics(Map<String, Object> map);

    Integer userStatistics(Map<String, Object> map);

    Integer orderStatistics(Map<String, Object> map);
    Integer orderTotalStatistics(LocalDateTime beginTimeTotal, LocalDateTime endTimeTotal, Integer status);

    List<DailyOrderStatistics> orderStatisticsByRange(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime
    );
}
