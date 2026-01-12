package com.example.top_hog_server.controller;

import com.example.top_hog_server.model.Order;
import com.example.top_hog_server.repository.OrderRepository;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import com.example.top_hog_server.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付控制器
 */
@RestController
@RequestMapping("/api/payment")
@Tag(name = "支付管理", description = "支付相关接口")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    @Operation(summary = "创建订单")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Long productId = Long.valueOf(request.get("productId").toString());

        try {
            Order order = paymentService.createOrder(user.getId(), productId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", order);
            response.put("message", "订单创建成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("创建订单失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发起支付
     */
    @PostMapping("/prepay")
    @Operation(summary = "发起支付")
    public ResponseEntity<?> prepay(@RequestBody Map<String, Object> request) {
        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        String orderNo = (String) request.get("orderNo");

        try {
            Map<String, String> payParams = paymentService.prepay(orderNo, user.getOpenid());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", payParams);
            response.put("message", "支付参数获取成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发起支付失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 支付回调(微信服务器调用)
     */
    @PostMapping("/notify")
    @Operation(summary = "支付回调")
    public ResponseEntity<?> paymentNotify(@RequestBody Map<String, Object> callbackData) {
        try {
            paymentService.handlePaymentCallback(callbackData);

            // 微信要求返回特定格式
            Map<String, String> response = new HashMap<>();
            response.put("code", "SUCCESS");
            response.put("message", "成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("处理支付回调失败", e);
            Map<String, String> response = new HashMap<>();
            response.put("code", "FAIL");
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 查询订单
     */
    @GetMapping("/order/{orderNo}")
    @Operation(summary = "查询订单")
    public ResponseEntity<?> getOrder(@PathVariable String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo).orElse(null);

        Map<String, Object> response = new HashMap<>();
        if (order != null) {
            response.put("success", true);
            response.put("data", order);
        } else {
            response.put("success", false);
            response.put("message", "订单不存在");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 我的订单列表
     */
    @GetMapping("/orders")
    @Operation(summary = "我的订单列表")
    public ResponseEntity<?> myOrders() {
        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }
}
