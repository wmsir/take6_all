package com.example.top_hog_server.service;

import com.example.top_hog_server.model.Order;
import com.example.top_hog_server.model.TransactionLog;
import com.example.top_hog_server.repository.OrderRepository;
import com.example.top_hog_server.repository.TransactionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 财务管理服务
 */
@Service
@Slf4j
public class FinanceService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    /**
     * 查询充值流水
     */
    public Map<String, Object> getRechargeRecords(LocalDateTime startTime, LocalDateTime endTime,
            String status, Integer page, Integer pageSize) {
        List<Order> allOrders;

        if (status != null && !status.isEmpty()) {
            allOrders = orderRepository.findAll().stream()
                    .filter(order -> order.getStatus().equals(status))
                    .filter(order -> isInTimeRange(order.getCreatedAt(), startTime, endTime))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        } else {
            allOrders = orderRepository.findAll().stream()
                    .filter(order -> isInTimeRange(order.getCreatedAt(), startTime, endTime))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        }

        // 分页
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, allOrders.size());
        List<Order> pagedOrders = allOrders.subList(start, end);

        // 统计
        BigDecimal totalAmount = allOrders.stream()
                .filter(order -> "PAID".equals(order.getStatus()))
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCount = allOrders.size();
        long paidCount = allOrders.stream()
                .filter(order -> "PAID".equals(order.getStatus()))
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("records", pagedOrders);
        result.put("total", totalCount);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalAmount", totalAmount);
        result.put("paidCount", paidCount);

        return result;
    }

    /**
     * 获取财务报表
     */
    public Map<String, Object> getFinanceReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> isInTimeRange(order.getCreatedAt(), startTime, endTime))
                .collect(Collectors.toList());

        // 总收入
        BigDecimal totalRevenue = orders.stream()
                .filter(order -> "PAID".equals(order.getStatus()))
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 订单统计
        long totalOrders = orders.size();
        long paidOrders = orders.stream().filter(o -> "PAID".equals(o.getStatus())).count();
        long pendingOrders = orders.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
        long failedOrders = orders.stream().filter(o -> "FAILED".equals(o.getStatus())).count();
        long refundedOrders = orders.stream().filter(o -> "REFUNDED".equals(o.getStatus())).count();

        // 退款金额
        BigDecimal refundAmount = orders.stream()
                .filter(order -> "REFUNDED".equals(order.getStatus()))
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 净收入
        BigDecimal netRevenue = totalRevenue.subtract(refundAmount);

        // 按日期分组统计
        Map<LocalDate, BigDecimal> dailyRevenue = orders.stream()
                .filter(order -> "PAID".equals(order.getStatus()))
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getAmount, BigDecimal::add)));

        // 按商品类型统计
        Map<String, Long> ordersByProduct = orders.stream()
                .filter(order -> "PAID".equals(order.getStatus()))
                .collect(Collectors.groupingBy(Order::getProductName, Collectors.counting()));

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalRevenue", totalRevenue);
        report.put("refundAmount", refundAmount);
        report.put("netRevenue", netRevenue);
        report.put("totalOrders", totalOrders);
        report.put("paidOrders", paidOrders);
        report.put("pendingOrders", pendingOrders);
        report.put("failedOrders", failedOrders);
        report.put("refundedOrders", refundedOrders);
        report.put("dailyRevenue", dailyRevenue);
        report.put("ordersByProduct", ordersByProduct);

        return report;
    }

    /**
     * 对账功能
     */
    public Map<String, Object> reconciliation(LocalDate date) {
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);

        // 获取当日订单
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> isInTimeRange(order.getCreatedAt(), startTime, endTime))
                .collect(Collectors.toList());

        // 获取当日交易记录
        List<TransactionLog> transactions = transactionLogRepository.findAll().stream()
                .filter(log -> isInTimeRange(log.getCreatedAt(), startTime, endTime))
                .collect(Collectors.toList());

        // 订单金额统计
        BigDecimal orderTotalAmount = orders.stream()
                .filter(order -> "PAID".equals(order.getStatus()))
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 充值记录统计
        List<TransactionLog> rechargeRecords = transactions.stream()
                .filter(log -> "RECHARGE".equals(log.getType()))
                .collect(Collectors.toList());

        // 消费记录统计
        List<TransactionLog> consumeRecords = transactions.stream()
                .filter(log -> "CONSUME".equals(log.getType()))
                .collect(Collectors.toList());

        // 检查订单和交易记录是否匹配
        List<String> unmatchedOrders = new ArrayList<>();
        for (Order order : orders) {
            if ("PAID".equals(order.getStatus())) {
                boolean matched = rechargeRecords.stream()
                        .anyMatch(log -> order.getOrderNo().equals(log.getOrderNo()));
                if (!matched) {
                    unmatchedOrders.add(order.getOrderNo());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("orderCount", orders.size());
        result.put("paidOrderCount", orders.stream().filter(o -> "PAID".equals(o.getStatus())).count());
        result.put("orderTotalAmount", orderTotalAmount);
        result.put("rechargeRecordCount", rechargeRecords.size());
        result.put("consumeRecordCount", consumeRecords.size());
        result.put("unmatchedOrders", unmatchedOrders);
        result.put("isBalanced", unmatchedOrders.isEmpty());

        return result;
    }

    /**
     * 退款管理
     */
    public Map<String, Object> getRefundRecords(LocalDateTime startTime, LocalDateTime endTime) {
        List<Order> refundOrders = orderRepository.findAll().stream()
                .filter(order -> "REFUNDED".equals(order.getStatus()))
                .filter(order -> isInTimeRange(order.getCreatedAt(), startTime, endTime))
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .collect(Collectors.toList());

        BigDecimal totalRefundAmount = refundOrders.stream()
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("records", refundOrders);
        result.put("total", refundOrders.size());
        result.put("totalRefundAmount", totalRefundAmount);

        return result;
    }

    /**
     * 导出财务数据(CSV格式)
     */
    public String exportFinanceData(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> isInTimeRange(order.getCreatedAt(), startTime, endTime))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("订单号,用户ID,商品名称,金额,状态,支付方式,交易ID,创建时间,支付时间\n");

        for (Order order : orders) {
            csv.append(order.getOrderNo()).append(",");
            csv.append(order.getUserId()).append(",");
            csv.append(order.getProductName()).append(",");
            csv.append(order.getAmount()).append(",");
            csv.append(order.getStatus()).append(",");
            csv.append(order.getPaymentMethod()).append(",");
            csv.append(order.getTransactionId() != null ? order.getTransactionId() : "").append(",");
            csv.append(order.getCreatedAt()).append(",");
            csv.append(order.getPaidAt() != null ? order.getPaidAt() : "").append("\n");
        }

        return csv.toString();
    }

    /**
     * 判断时间是否在范围内
     */
    private boolean isInTimeRange(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        if (time == null)
            return false;
        if (start != null && time.isBefore(start))
            return false;
        if (end != null && time.isAfter(end))
            return false;
        return true;
    }
}
