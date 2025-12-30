package com.example.top_hog_server.service;

import com.example.top_hog_server.model.User;
import com.example.top_hog_server.payload.dto.request.WechatLoginRequest;
import com.example.top_hog_server.payload.dto.response.WechatLoginResponse;
import com.example.top_hog_server.repository.UserRepository;
import com.example.top_hog_server.security.jwt.JwtUtils;
import com.example.top_hog_server.security.services.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
public class WechatService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    // 建议：正式上线请将这些配置放入 application.yml
    private static final String APP_ID = "wx4d79aa8fc3470971";
    private static final String APP_SECRET = "7ff4fae3ca994f587c5d27fcd579887b";

    public WechatLoginResponse login(WechatLoginRequest request) {
        // 1. 校验参数
        String code = request.getCode();
        if (code == null || code.isEmpty()) {
            throw new RuntimeException("Code不能为空");
        }

        // 2. 请求微信接口
        String wxLoginUrl = "https://api.weixin.qq.com/sns/jscode2session" +
                "?appid=" + APP_ID +
                "&secret=" + APP_SECRET +
                "&js_code=" + code +
                "&grant_type=authorization_code";

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(wxLoginUrl, String.class);

        try {
            // 3. 解析微信返回数据
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> wxResult = mapper.readValue(response, Map.class);

            if (wxResult.containsKey("errcode") && (Integer) wxResult.get("errcode") != 0) {
                throw new RuntimeException("微信登录失败: " + wxResult.get("errmsg"));
            }

            String openid = (String) wxResult.get("openid");
            String sessionKey = (String) wxResult.get("session_key");

            System.out.println("微信登录成功，OpenID: " + openid);

            // ==========================================
            // 4. 数据库逻辑 (适配 User.java)
            // ==========================================
            // 使用 findByWechatOpenid (匹配 User 实体属性 wechatOpenid)
            Optional<User> userOptional = userRepository.findByWechatOpenid(openid);
            User user;

            if (userOptional.isPresent()) {
                // A. 老用户：更新 sessionKey
                user = userOptional.get();
                user.setWechatSessionKey(sessionKey);
                userRepository.save(user);
            } else {
                // B. 新用户：注册
                user = new User();
                user.setWechatOpenid(openid);        // 设置 OpenID
                user.setWechatSessionKey(sessionKey); // 设置 SessionKey
                // 设置默认昵称 (截取 OpenID 后4位作为标识)
                String suffix = openid.length() > 4 ? openid.substring(openid.length() - 4) : "User";
                user.setNickname("玩家_" + suffix);
                // 注意：User.java 中 createdAt 设为 insertable=false，由数据库自动生成时间，
                // 所以这里不需要 user.setCreatedAt(...)

                // 保存并刷新，以便获取数据库自动生成的 ID 和 CreatedAt
                user = userRepository.save(user);
            }

            // ==========================================
            // 5. 生成 JWT Token
            // ==========================================
            UserDetailsImpl userDetails = UserDetailsImpl.build(user);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 使用 generateMiniAppToken 生成包含更多信息的 Token
            String jwt = jwtUtils.generateMiniAppToken(userDetails);

            // 6. 返回响应
            // 注意：如果是刚创建的新用户，user.getRegisterTime() 可能为 null (因为还没从数据库 refresh 回来)，
            // 这里做一个简单的空值处理，如果为空则返回当前时间
            Long registerTime = user.getRegisterTime();
            if (registerTime == null) {
                registerTime = System.currentTimeMillis();
            }

            return new WechatLoginResponse(
                    String.valueOf(user.getId()),
                    user.getNickname(),
                    user.getAvatarUrl(), // 可能为 null，前端需处理
                    jwt,
                    user.getPhone(),     // 可能为 null
                    registerTime
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("登录处理异常: " + e.getMessage());
        }
    }
}