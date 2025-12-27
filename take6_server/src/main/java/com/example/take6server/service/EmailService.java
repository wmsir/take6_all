package com.example.take6server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务类。
 * 负责发送邮件，例如验证码邮件。
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from}") // 从 application.properties 读取发件人邮箱
    private String fromEmail;

    /**
     * 发送验证码邮件。
     * @param to 收件人邮箱地址
     * @param code 要发送的验证码
     * @throws MailException 如果邮件发送失败
     */
    public void sendVerificationCode(String to, String code) throws MailException {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("您的邮箱验证码"); // 邮件主题
            message.setText("欢迎！您的验证码是: " + code + "\n此验证码将在10分钟后过期。"); // 邮件内容
            mailSender.send(message);
            logger.info("验证码邮件已发送至 {}", to);
        } catch (MailException e) {
            logger.error("发送验证码邮件至 {} 失败: {}", to, e.getMessage());
            throw e; // 重新抛出异常，以便上层调用者处理
        }
    }
}