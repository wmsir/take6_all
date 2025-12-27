package com.example.take6server.controller;


import com.example.take6server.exception.BusinessException;
import com.example.take6server.exception.ErrorCode;
import com.example.take6server.model.User;
import com.example.take6server.payload.dto.request.*;
import com.example.take6server.payload.dto.response.ApiResponse;
import com.example.take6server.payload.dto.response.JwtResponse;
import com.example.take6server.payload.dto.response.WechatLoginResponse;
import com.example.take6server.repository.UserRepository;
import com.example.take6server.security.jwt.JwtUtils;
import com.example.take6server.security.services.UserDetailsImpl;
import com.example.take6server.service.EmailService;
import com.example.take6server.service.VerificationCodeService;
import com.example.take6server.service.WechatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth") // 此控制器下所有接口的基础路径为 /api/auth
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager; // Spring Security认证管理器

    @Autowired
    private UserRepository userRepository; // 用户数据仓库

    @Autowired
    private PasswordEncoder encoder; // 密码编码器

    @Autowired
    private JwtUtils jwtUtils; // JWT工具类

    @Autowired
    private EmailService emailService; // 邮件服务

    @Autowired
    private VerificationCodeService verificationCodeService; // 验证码服务

    @Autowired
    private WechatService wechatService;

    /**
     * 验证Token有效性接口。
     * 需要携带有效Token访问。如果Token无效，会被Filter拦截返回401。
     * 如果到达此接口，说明Token有效。
     */
    @PostMapping("/validate")
    public ApiResponse<String> validateToken() {
        return ApiResponse.success("Token is valid");
    }

    /**
     * 微信登录接口
     */
    @PostMapping("/wechat/login")
    public ApiResponse<WechatLoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.success(wechatService.login(request));
    }

    /**
     * 获取手机验证码 (Mock)
     */
    @PostMapping("/phone/code")
    public ApiResponse<Map<String, Object>> getPhoneCode(@Valid @RequestBody PhoneCodeRequest request) {
        // Mock sending code
        Map<String, Object> data = new HashMap<>();
        data.put("expireIn", 60);
        return ApiResponse.success(data);
    }

    /**
     * 绑定手机号
     */
    @PostMapping("/bind/phone")
    public ApiResponse<Map<String, String>> bindPhone(@Valid @RequestBody BindPhoneRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));

        // In real app, verify code here. For now, assume code is valid.
        user.setPhone(request.getPhone());
        userRepository.save(user);

        Map<String, String> data = new HashMap<>();
        data.put("phone", user.getPhone());
        return ApiResponse.success(data);
    }

    /**
     * 用户登录接口。
     * @param loginRequest 包含用户名和密码的登录请求体
     * @return 包含JWT令牌和用户信息的ApiResponse
     * @throws BusinessException 如果认证失败或发生其他业务错误
     */
    @PostMapping("/signin")
    public ApiResponse<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            // 将认证信息存入SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 生成JWT令牌
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // 检查邮箱是否已验证 (此处userRepository.findById是示例，UserDetails中已有ID)
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "尝试登录的用户在数据库中未找到"));

            if (!user.isEmailVerified()) {
                logger.warn("用户 {} (邮箱: {}) 尝试使用未验证的邮箱登录", userDetails.getUsername(), userDetails.getEmail());
                throw new BusinessException(ErrorCode.USER_NOT_VERIFIED, "请先验证您的邮箱后再登录。");
            }

            JwtResponse jwtResponse = new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getNickname(),
                    userDetails.getAvatarUrl(),
                    userDetails.getPhone(),
                    userDetails.getInviteCode(),
                    userDetails.getVipStatus());
            logger.info("用户 {} 登录成功", loginRequest.getUsername());
            return ApiResponse.success(jwtResponse);
        } catch (BadCredentialsException e) {
            logger.warn("用户 {} 登录认证失败: 用户名或密码错误", loginRequest.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        } catch (BusinessException e) { // 捕获如 USER_NOT_VERIFIED 等业务异常
            throw e; // 直接重新抛出，由GlobalExceptionHandler处理
        } catch (Exception e) {
            logger.error("用户 {} 登录时发生内部错误: {}", loginRequest.getUsername(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录时发生内部错误，请稍后重试。");
        }
    }

    /**
     * 请求发送邮箱验证码接口。
     * @param emailRequest 包含邮箱地址的请求体
     * @return 操作结果的ApiResponse，成功时data为提示信息
     * @throws BusinessException 如果邮箱已注册并验证，或邮件发送失败
     */
    @PostMapping("/request-verification-code")
    public ApiResponse<String> requestVerificationCode(@Valid @RequestBody EmailRequest emailRequest) {
        // 检查邮箱是否已注册并验证
        userRepository.findByEmail(emailRequest.getEmail()).ifPresent(user -> { // 使用了新增的findByEmail
            if (user.isEmailVerified()) {
                logger.warn("邮箱 {} 已注册并验证，请求发送验证码被拒绝", emailRequest.getEmail());
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS,"此邮箱已注册并验证。");
            }
        });

        String code = verificationCodeService.generateAndStoreCode(emailRequest.getEmail());
        try {
            emailService.sendVerificationCode(emailRequest.getEmail(), code);
            String successMessage = "验证码已发送至 " + emailRequest.getEmail() + "，有效期10分钟。";
            logger.info(successMessage);
            return ApiResponse.success(successMessage, successMessage); // data可以设为消息本身或null
        } catch (Exception e) {
            logger.error("向邮箱 {} 发送验证码失败: {}", emailRequest.getEmail(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发送验证码失败，请稍后重试。");
        }
    }

    /**
     * 用户注册接口。
     * @param signUpRequest 包含注册信息的请求体 (用户名、邮箱、密码、验证码)
     * @return 操作结果的ApiResponse，成功时data为提示信息
     * @throws BusinessException 如果验证码无效、用户名或邮箱已存在，或发生其他业务错误
     */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED) // 成功创建用户时返回201状态码
    public ApiResponse<String> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        // 1. 校验邮箱验证码
        if (!verificationCodeService.verifyCode(signUpRequest.getEmail(), signUpRequest.getEmailVerificationCode())) {
            logger.warn("用户注册失败 (邮箱: {}): 无效或已过期的验证码", signUpRequest.getEmail());
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE,"无效或已过期的验证码，如果需要请重新获取。");
        }

        // 2. 检查用户名是否已存在
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            logger.warn("用户注册失败: 用户名 {} 已被占用", signUpRequest.getUsername());
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS,"用户名已被占用!");
        }

        // 3. 检查邮箱是否已存在
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            logger.warn("用户注册失败: 邮箱 {}已被使用", signUpRequest.getEmail());
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS,"邮箱已被使用!");
        }

        // 4. 创建新用户账号
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));
        user.setNickname(signUpRequest.getUsername()); // 默认昵称为用户名
        user.setInviteCode(generateInviteCode());      // 生成邀请码
        user.setVipStatus(0);                          // 设置默认VIP状态
        user.setEmailVerified(true);                   // 邮箱已通过验证码校验

        userRepository.save(user);
        String successMessage = "用户 " + signUpRequest.getUsername() + " 注册成功!";
        logger.info(successMessage);
        return ApiResponse.success(successMessage, successMessage);
    }

    /**
     * 用户登出接口。
     * 对于JWT，主要是客户端清除Token。服务端可选择清除SecurityContext。
     * @return 操作结果的ApiResponse
     */
    @PostMapping("/signout")
    public ApiResponse<String> logoutUser() {
        SecurityContextHolder.clearContext(); // 清除当前线程的SecurityContext
        logger.info("用户已登出");
        String successMessage = "退出登录成功!";
        return ApiResponse.success(successMessage, successMessage);
    }

    /**
     * 生成随机邀请码的私有辅助方法。
     * @return 6位大写字母和数字组成的邀请码
     */
    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}