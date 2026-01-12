package com.example.top_hog_server.controller;

import com.example.top_hog_server.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GM管理控制器 - 用户管理
 * 需要管理员权限
 */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "GM用户管理", description = "GM后台用户管理接口(需要管理员权限)")
@Slf4j
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * 获取用户列表
     */
    @GetMapping
    @Operation(summary = "获取用户列表")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        try {
            Map<String, Object> result = adminUserService.getUserList(keyword, page, pageSize);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    @Operation(summary = "获取用户详情")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserDetail(@PathVariable Long userId) {
        try {
            Map<String, Object> detail = adminUserService.getUserDetail(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", detail);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 封禁用户
     */
    @PostMapping("/{userId}/ban")
    @Operation(summary = "封禁用户")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> banUser(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {

        try {
            String reason = request.getOrDefault("reason", "违规操作");
            adminUserService.banUser(userId, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户已封禁");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("封禁用户失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 解封用户
     */
    @PostMapping("/{userId}/unban")
    @Operation(summary = "解封用户")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unbanUser(@PathVariable Long userId) {
        try {
            adminUserService.unbanUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户已解封");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("解封用户失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 调整用户余额
     */
    @PostMapping("/{userId}/adjust-balance")
    @Operation(summary = "调整用户余额(补偿/扣除)")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adjustBalance(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request) {

        try {
            String currency = (String) request.get("currency"); // COINS or DIAMONDS
            Long amount = Long.valueOf(request.get("amount").toString());
            String reason = (String) request.getOrDefault("reason", "GM调整");

            adminUserService.adjustBalance(userId, currency, amount, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "余额调整成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("调整用户余额失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户充值记录
     */
    @GetMapping("/{userId}/recharge-history")
    @Operation(summary = "获取用户充值记录")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserRechargeHistory(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> history = adminUserService.getUserRechargeHistory(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户充值记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
