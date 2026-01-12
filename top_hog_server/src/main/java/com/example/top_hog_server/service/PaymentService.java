package com.example.top_hog_server.service;

import com.example.top_hog_server.exception.BusinessException;
import com.example.top_hog_server.exception.ErrorCode;
import com.example.top_hog_server.model.*;
import com.example.top_hog_server.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 支付服务
 */
@Service
@Slf4j
public class PaymentService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.pay.mchid:}")
    private String mchId;

    @Value("${wechat.pay.notify-url:}")
    private String notifyUrl;

    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(Long userId, Long productId) {
        // 查询商品
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"));

        if (!"ACTIVE".equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "商品已下架");
        }

        // 生成订单号
        String orderNo = generateOrderNo();

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setProductName(product.getName());
        order.setAmount(product.getPrice());
        order.setStatus("PENDING");
        order.setPaymentMethod("WECHAT");

        orderRepository.save(order);

        log.info("创建订单成功: orderNo={}, userId={}, productId={}", orderNo, userId, productId);
        return order;
    }

    /**
     * JSAPI下单
     * 注意: 这是简化版本,实际需要对接微信支付API
     */
    public Map<String, String> prepay(String orderNo, String openid) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "订单状态异常");
        }

        // TODO: 实际对接微信支付API
        // 1. 构建请求参数
        // 2. 签名
        // 3. 调用统一下单接口
        // 4. 返回支付参数给前端

        Map<String, String> payParams = new HashMap<>();
        payParams.put("appId", appId);
        payParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        payParams.put("nonceStr", UUID.randomUUID().toString().replace("-", ""));
        payParams.put("package", "prepay_id=mock_prepay_id"); // 实际应从微信获取
        payParams.put("signType", "MD5");
        payParams.put("paySign", "mock_sign"); // 实际应计算签名

        log.warn("JSAPI下单 - 当前为模拟实现,请对接真实微信支付API");
        return payParams;
    }

    /**
     * 处理支付回调
     */
    @Transactional
    public void handlePaymentCallback(Map<String, Object> callbackData) {
        // TODO: 验证签名

        String orderNo = (String) callbackData.get("out_trade_no");
        String transactionId = (String) callbackData.get("transaction_id");

        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));

        if ("PAID".equals(order.getStatus())) {
            log.warn("订单已支付,忽略重复回调: orderNo={}", orderNo);
            return;
        }

        // 更新订单状态
        order.setStatus("PAID");
        order.setTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // 充值到余额
        rechargeBalance(order.getUserId(), order);

        log.info("支付成功: orderNo={}, transactionId={}", orderNo, transactionId);
    }

    /**
     * 充值到余额
     */
    @Transactional
    public void rechargeBalance(Long userId, Order order) {
        // 查询商品
        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"));

        // 获取或创建用户余额
        UserBalance balance = userBalanceRepository.findById(userId)
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUserId(userId);
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
                    newBalance.setUser(user);
                    return newBalance;
                });

        // 充值金币
        if (product.getCoins() > 0) {
            Long oldCoins = balance.getCoins();
            balance.setCoins(oldCoins + product.getCoins());

            // 记录交易日志
            TransactionLog txLog = new TransactionLog();
            txLog.setUserId(userId);
            txLog.setType("RECHARGE");
            txLog.setCurrency("COINS");
            txLog.setAmount(product.getCoins());
            txLog.setBalanceBefore(oldCoins);
            txLog.setBalanceAfter(balance.getCoins());
            txLog.setReason("充值-" + product.getName());
            txLog.setOrderNo(order.getOrderNo());
            transactionLogRepository.save(txLog);
        }

        // 充值钻石
        if (product.getDiamonds() > 0) {
            Long oldDiamonds = balance.getDiamonds();
            balance.setDiamonds(oldDiamonds + product.getDiamonds());

            // 记录交易日志
            TransactionLog txLog = new TransactionLog();
            txLog.setUserId(userId);
            txLog.setType("RECHARGE");
            txLog.setCurrency("DIAMONDS");
            txLog.setAmount(product.getDiamonds());
            txLog.setBalanceBefore(oldDiamonds);
            txLog.setBalanceAfter(balance.getDiamonds());
            txLog.setReason("充值-" + product.getName());
            txLog.setOrderNo(order.getOrderNo());
            transactionLogRepository.save(txLog);
        }

        // 开通VIP
        if (product.getVipDays() > 0) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime vipExpireTime = user.getVipExpireTime();

            if (vipExpireTime == null || vipExpireTime.isBefore(now)) {
                // 首次开通或已过期
                user.setVipExpireTime(now.plusDays(product.getVipDays()));
            } else {
                // 续费
                user.setVipExpireTime(vipExpireTime.plusDays(product.getVipDays()));
            }

            user.setVip(true);
            userRepository.save(user);
        }

        // 更新累计充值金额
        balance.setTotalRecharge(balance.getTotalRecharge().add(order.getAmount()));
        userBalanceRepository.save(balance);

        log.info("充值成功: userId={}, coins={}, diamonds={}, vipDays={}",
                userId, product.getCoins(), product.getDiamonds(), product.getVipDays());
    }

    /**
     * 消费金币
     */

    /**
     * 增加金币(系统奖励)
     */
    @Transactional
    public void addCoins(Long userId, Integer amount, String reason) {
        addCoins(userId, amount.longValue(), reason);
    }

    @Transactional
    public void addCoins(Long userId, Long amount, String reason) {
        UserBalance balance = userBalanceRepository.findById(userId)
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUserId(userId);
                    // 尝试查找用户确保用户存在
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
                    newBalance.setUser(user);
                    return newBalance;
                });

        Long oldCoins = balance.getCoins();
        balance.setCoins(oldCoins + amount);
        userBalanceRepository.save(balance);

        // 记录交易日志
        TransactionLog txLog = new TransactionLog();
        txLog.setUserId(userId);
        txLog.setType("REWARD"); // 奖励类型
        txLog.setCurrency("COINS");
        txLog.setAmount(amount);
        txLog.setBalanceBefore(oldCoins);
        txLog.setBalanceAfter(balance.getCoins());
        txLog.setReason(reason);
        transactionLogRepository.save(txLog);
    }

    /**
     * 增加钻石(系统奖励)
     */
    @Transactional
    public void addDiamonds(Long userId, Integer amount, String reason) {
        addDiamonds(userId, amount.longValue(), reason);
    }

    @Transactional
    public void addDiamonds(Long userId, Long amount, String reason) {
        UserBalance balance = userBalanceRepository.findById(userId)
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUserId(userId);
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
                    newBalance.setUser(user);
                    return newBalance;
                });

        Long oldDiamonds = balance.getDiamonds();
        balance.setDiamonds(oldDiamonds + amount);
        userBalanceRepository.save(balance);

        // 记录交易日志
        TransactionLog txLog = new TransactionLog();
        txLog.setUserId(userId);
        txLog.setType("REWARD");
        txLog.setCurrency("DIAMONDS");
        txLog.setAmount(amount);
        txLog.setBalanceBefore(oldDiamonds);
        txLog.setBalanceAfter(balance.getDiamonds());
        txLog.setReason(reason);
        transactionLogRepository.save(txLog);
    }

    /**
     * 消费金币
     */
    @Transactional
    public void consumeCoins(Long userId, Long amount, String reason) {
        UserBalance balance = userBalanceRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "余额不存在"));

        if (balance.getCoins() < amount) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "金币余额不足");
        }

        Long oldCoins = balance.getCoins();
        balance.setCoins(oldCoins - amount);
        userBalanceRepository.save(balance);

        // 记录交易日志
        TransactionLog txLog = new TransactionLog();
        txLog.setUserId(userId);
        txLog.setType("CONSUME");
        txLog.setCurrency("COINS");
        txLog.setAmount(amount);
        txLog.setBalanceBefore(oldCoins);
        txLog.setBalanceAfter(balance.getCoins());
        txLog.setReason(reason);
        transactionLogRepository.save(txLog);

        log.info("消费金币: userId={}, amount={}, reason={}", userId, amount, reason);
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORDER" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
