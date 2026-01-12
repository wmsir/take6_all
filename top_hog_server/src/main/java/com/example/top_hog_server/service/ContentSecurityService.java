package com.example.top_hog_server.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 内容安全服务
 * 对接微信内容安全API
 */
@Service
@Slf4j
public class ContentSecurityService {

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;
    private long tokenExpireTime = 0;

    /**
     * 获取access_token
     */
    private String getAccessToken() {
        long currentTime = System.currentTimeMillis();

        // 如果token未过期,直接返回
        if (accessToken != null && currentTime < tokenExpireTime) {
            return accessToken;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = String.format(
                    "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                    appId, appSecret);

            HttpPost httpPost = new HttpPost(url);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = new String(response.getEntity().getContent().readAllBytes(),
                        StandardCharsets.UTF_8);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                if (jsonNode.has("access_token")) {
                    accessToken = jsonNode.get("access_token").asText();
                    int expiresIn = jsonNode.get("expires_in").asInt();
                    tokenExpireTime = currentTime + (expiresIn - 300) * 1000L; // 提前5分钟过期
                    return accessToken;
                } else {
                    log.error("获取access_token失败: {}", responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("获取access_token异常", e);
            return null;
        }
    }

    /**
     * 文本内容安全检测
     * 
     * @param content 待检测文本
     * @param openid  用户openid
     * @return true-安全, false-违规
     */
    public boolean checkText(String content, String openid) {
        if (content == null || content.trim().isEmpty()) {
            return true;
        }

        String token = getAccessToken();
        if (token == null) {
            log.warn("无法获取access_token,跳过内容检测");
            return true; // 降级策略:检测失败时放行
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = "https://api.weixin.qq.com/wxa/msg_sec_check?access_token=" + token;

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("content", content);
            requestBody.put("version", 2);
            requestBody.put("scene", 1); // 1-资料, 2-评论, 3-论坛, 4-社交日志
            requestBody.put("openid", openid);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = new String(response.getEntity().getContent().readAllBytes(),
                        StandardCharsets.UTF_8);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                int errcode = jsonNode.get("errcode").asInt();

                if (errcode == 0) {
                    // 检测通过
                    return true;
                } else if (errcode == 87014) {
                    // 内容含有违法违规内容
                    log.warn("文本内容违规: {}", content);
                    return false;
                } else {
                    log.error("文本检测失败: {}", responseBody);
                    return true; // 降级策略
                }
            }
        } catch (Exception e) {
            log.error("文本内容检测异常", e);
            return true; // 降级策略
        }
    }

    /**
     * 图片内容安全检测
     * 
     * @param imageUrl 图片URL
     * @param openid   用户openid
     * @return true-安全, false-违规
     */
    public boolean checkImage(String imageUrl, String openid) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return true;
        }

        String token = getAccessToken();
        if (token == null) {
            log.warn("无法获取access_token,跳过图片检测");
            return true;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = "https://api.weixin.qq.com/wxa/img_sec_check?access_token=" + token;

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("media_url", imageUrl);
            requestBody.put("media_type", 2); // 1-音频, 2-图片
            requestBody.put("version", 2);
            requestBody.put("scene", 1);
            requestBody.put("openid", openid);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = new String(response.getEntity().getContent().readAllBytes(),
                        StandardCharsets.UTF_8);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                int errcode = jsonNode.get("errcode").asInt();

                if (errcode == 0) {
                    return true;
                } else if (errcode == 87014) {
                    log.warn("图片内容违规: {}", imageUrl);
                    return false;
                } else {
                    log.error("图片检测失败: {}", responseBody);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("图片内容检测异常", e);
            return true;
        }
    }
}
