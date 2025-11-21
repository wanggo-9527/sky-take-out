package com.sky.service.impl;

import com.sky.mapper.ReportMapper;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private WorkspaceService workspaceService;

    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        List<Double> turnoverList = new ArrayList<>();
        while (!begin.isEqual(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);
        String dateListString = StringUtils.join(dateList, ",");
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String, Object> map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", 5);
            Double turnover = reportMapper.turnoverStatistics(map);
            if (turnover == null) {
                turnover = 0.0;
            }
            turnoverList.add(turnover);
        }
        String turnoverListString = StringUtils.join(turnoverList, ",");
        return TurnoverReportVO.builder()
                .dateList(dateListString)
                .turnoverList(turnoverListString)
                .build();
    }

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        while (!begin.isEqual(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);
        String dateListString = StringUtils.join(dateList, ",");
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String, Object> map = new HashMap<>();
            map.put("endTime", endTime);
            map.put("status", 5);
            Integer totalUser = reportMapper.userStatistics(map);
            if (totalUser == null) {
                totalUser = 0;
            }
            totalUserList.add(totalUser);
            map.put("beginTime", beginTime);
            Integer newUser = reportMapper.userStatistics(map);
            if (newUser == null) {
                newUser = 0;
            }
            newUserList.add(newUser);

        }
        String totalUserListString = StringUtils.join(totalUserList, ",");
        String newUserListString = StringUtils.join(newUserList, ",");
        return UserReportVO.builder()
                .dateList(dateListString)
                .totalUserList(totalUserListString)
                .newUserList(newUserListString)
                .build();
            }
//订单统计
 /*   @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        LocalDateTime beginTimeTotal = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTimeTotal = LocalDateTime.of(end, LocalTime.MAX);
        Integer DayOrderCount = reportMapper.orderTotalStatistics(beginTimeTotal,endTimeTotal,null);
        Integer ValidOrderCount = reportMapper.orderTotalStatistics( beginTimeTotal, endTimeTotal,5);
        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> everyDayValidOrderCountList = new ArrayList<>();
        List<Integer> everyDayOrderCountList = new ArrayList<>();
        while (!begin.isEqual(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);
        String dateListString = StringUtils.join(dateList, ",");
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String, Object> map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            Integer everyDayOrderCount = reportMapper.orderStatistics(map);
            if (everyDayOrderCount == null) {
                everyDayOrderCount = 0;
            }
            everyDayOrderCountList.add(everyDayOrderCount);
            map.put("status", 5);
            Integer everyDayValidOrderCount = reportMapper.orderStatistics(map);
            if (everyDayValidOrderCount == null) {
                everyDayValidOrderCount = 0;
            }
            everyDayValidOrderCountList.add(everyDayValidOrderCount);
        }
        String everyDayOrderCountListString = StringUtils.join(everyDayOrderCountList, ",");
        String everyDayValidOrderCountListString = StringUtils.join(everyDayValidOrderCountList, ",");
        return OrderReportVO.builder()
                .dateList(dateListString)
                .orderCountList(everyDayOrderCountListString)
                .validOrderCountList(everyDayValidOrderCountListString)
                .totalOrderCount(DayOrderCount)
                .validOrderCount(ValidOrderCount)
                .orderCompletionRate(ValidOrderCount.doubleValue()/DayOrderCount.doubleValue())
                .build();

    }*/
    //订单统计
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        // 1. 生成完整的日期列表（从 begin 到 end，每天一条）
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate d = begin; !d.isAfter(end); d = d.plusDays(1)) {
            dateList.add(d);
        }
        String dateListString = StringUtils.join(dateList, ",");

        // 2. 一次性查出整个时间段内，每天的总订单数 + 有效订单数
        LocalDateTime beginTimeTotal = begin.atStartOfDay();
        LocalDateTime endTimeTotal = end.atTime(LocalTime.MAX);

        List<DailyOrderStatistics> statsList =
                reportMapper.orderStatisticsByRange(beginTimeTotal, endTimeTotal);

        Map<LocalDate, DailyOrderStatistics> statsMap = new HashMap<>();
        for (DailyOrderStatistics s : statsList) {
            statsMap.put(s.getOrderDate(), s);
        }

        List<Integer> everyDayOrderCountList = new ArrayList<>();
        List<Integer> everyDayValidOrderCountList = new ArrayList<>();
        int totalOrderCount = 0;
        int validOrderCount = 0;

        for (LocalDate date : dateList) {

            DailyOrderStatistics s = statsMap.get(date); // 这一天的统计结果（可能为 null）

            int dayTotal = 0;
            int dayValid = 0;

            if (s != null) {
                if (s.getTotalCount() != null) {
                    dayTotal = s.getTotalCount();
                }
                if (s.getValidCount() != null) {
                    dayValid = s.getValidCount();
                }
            }

            everyDayOrderCountList.add(dayTotal);
            everyDayValidOrderCountList.add(dayValid);

            totalOrderCount += dayTotal;
            validOrderCount += dayValid;
        }

        // 5. 计算完成率，防止除 0
        double completionRate = 0.0;
        if (totalOrderCount > 0) {
            completionRate = validOrderCount * 1.0 / totalOrderCount;
        }

        // 6. 转成字符串，兼容你现在的 VO 结构
        String everyDayOrderCountListString = StringUtils.join(everyDayOrderCountList, ",");
        String everyDayValidOrderCountListString = StringUtils.join(everyDayValidOrderCountList, ",");

        return OrderReportVO.builder()
                .dateList(dateListString)
                .orderCountList(everyDayOrderCountListString)
                .validOrderCountList(everyDayValidOrderCountListString)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(completionRate)
                .build();
    }

    @Override
    public Void exportBusinessData(HttpServletResponse response) {
        //1、查询营业数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        LocalDateTime beginTime= LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime= LocalDateTime.of(end, LocalTime.MAX);
        BusinessDataVO businessData = workspaceService.getBusinessData(beginTime, endTime);

        //2、通过poi 生成excel
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");//获取模板
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(in);
            XSSFSheet sheet = workbook.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间：" + begin + "至" + end);
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());

            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                BusinessDataVO dailyData = workspaceService.getBusinessData(
                        LocalDateTime.of(date, LocalTime.MIN),
                        LocalDateTime.of(date, LocalTime.MAX)
                );
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(dailyData.getTurnover());
                row.getCell(3).setCellValue(dailyData.getValidOrderCount());
                row.getCell(4).setCellValue(dailyData.getOrderCompletionRate());
                row.getCell(5).setCellValue(dailyData.getUnitPrice());
                row.getCell(6).setCellValue(dailyData.getNewUsers());
            }

            //3、通过输出流下载到浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);  // 修复点：原 excel 改为 workbook
            outputStream.close();
            workbook.close();              // 修复点：同上
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

}
