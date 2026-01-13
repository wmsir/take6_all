package com.top_hog.server.service;

import com.top_hog.server.entity.RealNameInfo;
import com.top_hog.server.repository.RealNameInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合规服务 (Compliance Service)
 * 处理实名认证和防沉迷逻辑
 */
@Service
@Slf4j
public class ComplianceService {

    @Autowired
    private RealNameInfoRepository realNameInfoRepository;

    /**
     * 提交实名认证
     * 
     * @param userId   用户ID
     * @param realName 真实姓名
     * @param idCard   身份证号
     * @return 是否成功
     */
    public boolean submitRealNameAuth(Long userId, String realName, String idCard) {
        if (realNameInfoRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("该用户已进行过实名认证");
        }

        // 简单校验身份证格式
        if (!isValidIdCard(idCard)) {
            throw new RuntimeException("身份证格式不正确");
        }

        int age = calculateAge(idCard);
        boolean isAdult = age >= 18;

        RealNameInfo info = new RealNameInfo();
        info.setUserId(userId);
        info.setRealName(realName); // TODO: Encrypt
        info.setIdCard(idCard); // TODO: Encrypt
        info.setAge(age);
        info.setIsAdult(isAdult);
        info.setIsVerified(true); // 简化逻辑：提交即视为通过

        realNameInfoRepository.save(info);
        log.info("User {} completed real-name auth. Age: {}, Adult: {}", userId, age, isAdult);
        return true;
    }

    /**
     * 检查是否允许进入游戏 (防沉迷检查)
     * 
     * @param userId 用户ID
     * @return true=允许, false=被限制
     */
    public boolean checkPlayRestriction(Long userId) {
        RealNameInfo info = realNameInfoRepository.findByUserId(userId).orElse(null);

        // 未认证用户：允许试玩 (通常限制1小时，这里简化为允许)
        // 实际上应强制实名，或者限制未实名只能玩1小时
        if (info == null) {
            // return false; // 严格模式
            return true; // 宽松模式
        }

        if (info.getIsAdult()) {
            return true; // 成年人无限制
        }

        // 未成年人检查
        LocalDateTime now = LocalDateTime.now();
        LocalTime time = now.toLocalTime();

        // 1. 宵禁检查 (22:00 - 08:00 禁止游戏)
        if (time.isAfter(LocalTime.of(22, 0)) || time.isBefore(LocalTime.of(8, 0))) {
            log.info("Minor {} restricted by curfew", userId);
            return false;
        }

        // 2. 时长检查 (周五六日及法定节假日每晚20时-21时各1小时)
        // 这里简化逻辑：只允许 20:00 - 21:00
        if (time.isBefore(LocalTime.of(20, 0)) || time.isAfter(LocalTime.of(21, 0))) {
            log.info("Minor {} restricted by time slot", userId);
            return false;
        }

        return true;
    }

    private boolean isValidIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18)
            return false;
        // 简单正则
        String regex = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";
        return idCard.matches(regex);
    }

    private int calculateAge(String idCard) {
        if (idCard == null || idCard.length() < 14)
            return 0;
        String yearStr = idCard.substring(6, 10);
        int birthYear = Integer.parseInt(yearStr);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        return currentYear - birthYear;
    }
}
