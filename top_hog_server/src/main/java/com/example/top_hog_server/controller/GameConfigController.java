package com.example.top_hog_server.controller;

import com.example.top_hog_server.model.GameConfiguration;
import com.example.top_hog_server.model.GameType;
import com.example.top_hog_server.service.GameConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 游戏配置管理控制器
 * 提供游戏配置的查询和管理接口
 */
@RestController
@RequestMapping("/api/game-config")
@Tag(name = "游戏配置", description = "游戏配置管理相关接口")
public class GameConfigController {
    
    private final GameConfigurationService configService;
    
    @Autowired
    public GameConfigController(GameConfigurationService configService) {
        this.configService = configService;
    }
    
    /**
     * 获取所有可用的游戏类型
     */
    @GetMapping("/types")
    @Operation(summary = "获取所有游戏类型", description = "返回系统支持的所有游戏类型枚举")
    public ResponseEntity<List<Map<String, String>>> getAllGameTypes() {
        List<Map<String, String>> gameTypes = Arrays.stream(GameType.values())
            .map(type -> {
                Map<String, String> map = new HashMap<>();
                map.put("code", type.getCode());
                map.put("displayName", type.getDisplayName());
                map.put("description", type.getDescription());
                return map;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(gameTypes);
    }
    
    /**
     * 获取所有已启用的游戏配置
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取启用的游戏", description = "返回所有已启用的游戏配置列表")
    public ResponseEntity<List<GameConfiguration>> getEnabledGames() {
        List<GameConfiguration> configs = configService.getAllEnabledGames();
        return ResponseEntity.ok(configs);
    }
    
    /**
     * 根据游戏类型代码获取配置
     */
    @GetMapping("/{gameTypeCode}")
    @Operation(summary = "获取游戏配置", description = "根据游戏类型代码获取详细配置")
    public ResponseEntity<GameConfiguration> getGameConfig(@PathVariable String gameTypeCode) {
        return configService.getGameConfiguration(gameTypeCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建或更新游戏配置（管理员接口）
     */
    @PostMapping
    @Operation(summary = "保存游戏配置", description = "创建或更新游戏配置（需要管理员权限）")
    public ResponseEntity<GameConfiguration> saveConfiguration(@RequestBody GameConfiguration config) {
        GameConfiguration saved = configService.saveOrUpdateConfiguration(config);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * 启用或禁用游戏（管理员接口）
     */
    @PutMapping("/{gameTypeCode}/enabled")
    @Operation(summary = "启用/禁用游戏", description = "设置游戏的启用状态（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> setGameEnabled(
            @PathVariable String gameTypeCode,
            @RequestParam boolean enabled) {
        boolean success = configService.setGameEnabled(gameTypeCode, enabled);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("gameTypeCode", gameTypeCode);
        response.put("enabled", enabled);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 重新加载配置（热更新）
     */
    @PostMapping("/reload")
    @Operation(summary = "重新加载配置", description = "触发配置热更新（需要管理员权限）")
    public ResponseEntity<Map<String, String>> reloadConfigurations() {
        configService.reloadConfigurations();
        Map<String, String> response = new HashMap<>();
        response.put("message", "配置已重新加载");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除游戏配置（管理员接口）
     */
    @DeleteMapping("/{gameTypeCode}")
    @Operation(summary = "删除游戏配置", description = "删除指定的游戏配置（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> deleteConfiguration(@PathVariable String gameTypeCode) {
        boolean success = configService.deleteConfiguration(gameTypeCode);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("gameTypeCode", gameTypeCode);
        return ResponseEntity.ok(response);
    }
}
