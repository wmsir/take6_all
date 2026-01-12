package com.example.top_hog_server.controller;

import com.example.top_hog_server.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 财务管理控制器
 * 需要管理员权限
 */
@RestController
@RequestMapping("/api/finance")
@Tag(name = "财务管理", description = "财务相关接口(需要管理员权限)")
@Slf4j
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    /**
     * 查询充值流水
     */
    @GetMapping("/recharge-records")
    @Operation(summary = "查询充值流水")
    // @PreAuthorize("hasRole('ADMIN')") // 需要配置角色权限
    public ResponseEntity<?> getRechargeRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        try {
            Map<String, Object> result = financeService.getRechargeRecords(
                    startTime, endTime, status, page, pageSize);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询充值流水失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取财务报表
     */
    @GetMapping("/report")
    @Operation(summary = "获取财务报表")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getFinanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            Map<String, Object> report = financeService.getFinanceReport(startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", report);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取财务报表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 对账功能
     */
    @GetMapping("/reconciliation")
    @Operation(summary = "对账功能")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            Map<String, Object> result = financeService.reconciliation(date);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("对账失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 查询退款记录
     */
    @GetMapping("/refund-records")
    @Operation(summary = "查询退款记录")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRefundRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        try {
            Map<String, Object> result = financeService.getRefundRecords(startTime, endTime);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询退款记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 导出财务数据
     */
    @GetMapping("/export")
    @Operation(summary = "导出财务数据(CSV)")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportFinanceData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            String csvData = financeService.exportFinanceData(startDate, endDate);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment",
                    "finance_report_" + startDate + "_" + endDate + ".csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvData);
        } catch (Exception e) {
            log.error("导出财务数据失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 今日财务概览
     */
    @GetMapping("/today-summary")
    @Operation(summary = "今日财务概览")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTodaySummary() {
        try {
            LocalDate today = LocalDate.now();
            Map<String, Object> report = financeService.getFinanceReport(today, today);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", report);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取今日财务概览失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
