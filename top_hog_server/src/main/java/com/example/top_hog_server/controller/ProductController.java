package com.example.top_hog_server.controller;

import com.example.top_hog_server.model.Product;
import com.example.top_hog_server.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "商品管理", description = "商品相关接口")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    /**
     * 获取商品列表
     */
    @GetMapping
    @Operation(summary = "获取商品列表")
    public ResponseEntity<?> listProducts(@RequestParam(required = false) String type) {
        List<Product> products;

        if (type != null && !type.isEmpty()) {
            products = productRepository.findByTypeAndStatusOrderBySortOrderAsc(type, "ACTIVE");
        } else {
            products = productRepository.findByStatusOrderBySortOrderAsc("ACTIVE");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", products);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取商品详情")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElse(null);

        Map<String, Object> response = new HashMap<>();
        if (product != null) {
            response.put("success", true);
            response.put("data", product);
        } else {
            response.put("success", false);
            response.put("message", "商品不存在");
        }

        return ResponseEntity.ok(response);
    }
}
