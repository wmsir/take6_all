package com.example.top_hog_server.controller;

import com.example.top_hog_server.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息控制器
 */
@RestController
@RequestMapping("/api/messages")
@Tag(name = "消息推送", description = "站内信相关接口")
@Slf4j
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 发送系统公告(管理员)
     */
    @PostMapping("/system-announcement")
    @Operation(summary = "发送系统公告(需要管理员权限)")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendSystemAnnouncement(@RequestBody Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            String content = (String) request.get("content");
            Integer expireDays = request.get("expireDays") != null
                    ? Integer.valueOf(request.get("expireDays").toString())
                    : null;

            messageService.sendSystemAnnouncement(title, content, expireDays);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "系统公告已发送");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送系统公告失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送个人消息(管理员)
     */
    @PostMapping("/personal")
    @Operation(summary = "发送个人消息(需要管理员权限)")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendPersonalMessage(@RequestBody Map<String, Object> request) {
        try {
            Long receiverId = Long.valueOf(request.get("receiverId").toString());
            String title = (String) request.get("title");
            String content = (String) request.get("content");

            messageService.sendPersonalMessage(receiverId, title, content);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "个人消息已发送");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送个人消息失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取消息列表
     */
    @GetMapping
    @Operation(summary = "获取消息列表")
    public ResponseEntity<?> getMessages(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead) {

        try {
            Map<String, Object> result = messageService.getUserMessages(type, isRead);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取消息列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/{messageId}/read")
    @Operation(summary = "标记消息为已读")
    public ResponseEntity<?> markAsRead(@PathVariable Long messageId) {
        try {
            messageService.markAsRead(messageId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已标记为已读");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("标记消息失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 标记所有消息为已读
     */
    @PutMapping("/read-all")
    @Operation(summary = "标记所有消息为已读")
    public ResponseEntity<?> markAllAsRead() {
        try {
            messageService.markAllAsRead();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "所有消息已标记为已读");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("标记所有消息失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{messageId}")
    @Operation(summary = "删除消息")
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId) {
        try {
            messageService.deleteMessage(messageId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "消息已删除");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("删除消息失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息数")
    public ResponseEntity<?> getUnreadCount() {
        try {
            long count = messageService.getUnreadCount();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取未读消息数失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
