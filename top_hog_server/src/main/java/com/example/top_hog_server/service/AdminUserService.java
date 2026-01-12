package com.example.top_hog_server.service;

import com.example.top_hog_server.exception.BusinessException;
import com.example.top_hog_server.exception.ErrorCode;
import com.example.top_hog_server.model.User;
import com.example.top_hog_server.model.UserBalance;
import com.example.top_hog_server.model.TransactionLog;
import com.example.top_hog_server.repository.UserRepository;
import com.example.top_hog_server.repository.UserBalanceRepository;
import com.example.top_hog_server.repository.TransactionLogRepository;
import com.example.top_hog_server.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GM管理服务 - 用户管理
 */
@Service
@Slf4j
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 获取用户列表
     */
    public Map<String, Object> getUserList(String keyword, Integer page, Integer pageSize) {
        List<User> allUsers = userRepository.findAll();

        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            allUsers = allUsers.stream()
                    .filter(user -> (user.getNickname() != null && user.getNickname().toLowerCase().contains(kw)) ||
                            (user.getUsername() != null && user.getUsername().toLowerCase().contains(kw)) ||
                            (user.getPhone() != null && user.getPhone().contains(kw)) ||
                            user.getId().toString().contains(kw))
                    .collect(Collectors.toList());
        }

        // 排序(最新注册的在前)
        allUsers.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 分页
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, allUsers.size());
        List<User> pagedUsers = allUsers.subList(start, end);

        // 构建返回数据
        List<Map<String, Object>> userList = pagedUsers.stream()
                .map(this::buildUserInfo)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("users", userList);
        result.put("total", allUsers.size());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return result;
    }

    /**
     * 获取用户详情
     */
    public Map<String, Object> getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        Map<String, Object> detail = buildUserInfo(user);

        // 获取余额信息
        UserBalance balance = userBalanceRepository.findById(userId).orElse(null);
        if (balance != null) {
            detail.put("coins", balance.getCoins());
            detail.put("diamonds", balance.getDiamonds());
            detail.put("totalRecharge", balance.getTotalRecharge());
        } else {
            detail.put("coins", 0);
            detail.put("diamonds", 0);
            detail.put("totalRecharge", 0);
        }

        // 获取充值记录
        long rechargeCount = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "PAID").size();
        detail.put("rechargeCount", rechargeCount);

        // 获取最近交易记录
        List<TransactionLog> recentTransactions = transactionLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(10)
                .collect(Collectors.toList());
        detail.put("recentTransactions", recentTransactions);

        return detail;
    }

    /**
     * 封禁用户
     */
    @Transactional
    public void banUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        // 这里简单使用vipStatus字段,-1表示封禁
        // 实际应该添加专门的banned字段和banReason字段
        user.setVipStatus(-1);
        userRepository.save(user);

        log.info("用户被封禁: userId={}, reason={}", userId, reason);
    }

    /**
     * 解封用户
     */
    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        user.setVipStatus(0);
        userRepository.save(user);

        log.info("用户被解封: userId={}", userId);
    }

    /**
     * 修改用户余额(补偿)
     */
    @Transactional
    public void adjustBalance(Long userId, String currency, Long amount, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        // 获取或创建余额
        UserBalance balance = userBalanceRepository.findById(userId)
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUserId(userId);
                    newBalance.setUser(user);
                    return newBalance;
                });

        Long oldBalance;
        Long newBalance;

        if ("COINS".equals(currency)) {
            oldBalance = balance.getCoins();
            newBalance = oldBalance + amount;
            if (newBalance < 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "余额不足");
            }
            balance.setCoins(newBalance);
        } else if ("DIAMONDS".equals(currency)) {
            oldBalance = balance.getDiamonds();
            newBalance = oldBalance + amount;
            if (newBalance < 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "余额不足");
            }
            balance.setDiamonds(newBalance);
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "无效的货币类型");
        }

        userBalanceRepository.save(balance);

        // 记录交易日志
        TransactionLog txLog = new TransactionLog();
        txLog.setUserId(userId);
        txLog.setType(amount > 0 ? "REWARD" : "CONSUME");
        txLog.setCurrency(currency);
        txLog.setAmount(Math.abs(amount));
        txLog.setBalanceBefore(oldBalance);
        txLog.setBalanceAfter(newBalance);
        txLog.setReason("GM操作:" + reason);
        transactionLogRepository.save(txLog);

        log.info("GM调整用户余额: userId={}, currency={}, amount={}, reason={}",
                userId, currency, amount, reason);
    }

    /**
     * 获取用户充值记录
     */
    public List<Map<String, Object>> getUserRechargeHistory(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("orderNo", order.getOrderNo());
                    record.put("productName", order.getProductName());
                    record.put("amount", order.getAmount());
                    record.put("status", order.getStatus());
                    record.put("createdAt", order.getCreatedAt());
                    record.put("paidAt", order.getPaidAt());
                    return record;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建用户信息
     */
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("avatarUrl", user.getAvatarUrl());
        info.put("phone", user.getPhone());
        info.put("email", user.getEmail());
        info.put("vipStatus", user.getVipStatus());
        info.put("vipExpireTime", user.getVipExpireTime());
        info.put("isVip", user.isVip());
        info.put("isBanned", user.getVipStatus() == -1);
        info.put("createdAt", user.getCreatedAt());
        info.put("updatedAt", user.getUpdatedAt());
        return info;
    }
}
