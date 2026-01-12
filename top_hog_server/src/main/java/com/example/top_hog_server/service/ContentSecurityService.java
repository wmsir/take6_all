package com.example.top_hog_server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 内容安全服务
 * 对接微信内容安全API
 * 使用 java.net.http.HttpClient (Java 11+)
 */
@Service
@Slf4j
public class ContentSecurityService {

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    private String accessToken;
    private long tokenExpireTime = 0;

    public ContentSecurityService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 获取access_token
     */
    private String getAccessToken() {
        long currentTime = System.currentTimeMillis();

        if (accessToken != null && currentTime < tokenExpireTime) {
            return accessToken;
        }

        try {
            String url = String.format(
                    "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                    appId, appSecret);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                if (jsonNode.has("access_token")) {
                    accessToken = jsonNode.get("access_token").asText();
                    int expiresIn = jsonNode.get("expires_in").asInt();
                    tokenExpireTime = currentTime + (expiresIn - 300) * 1000L;
                    return accessToken;
                } else {
                    log.error("获取access_token失败: {}", response.body());
                    return null;
                }
            } else {
                log.error("获取access_token HTTP错误: {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("获取access_token异常", e);
            return null;
        }
    }

    /**
     * 文本内容安全检测
     */
    public boolean checkText(String content, String openid) {
        if (content == null || content.trim().isEmpty()) {
            return true;
        }

        String token = getAccessToken();
        if (token == null) {
            log.warn("无法获取access_token,跳过内容检测");
            return true;
        }

        try {
            String url = "https://api.weixin.qq.com/wxa/msg_sec_check?access_token=" + token;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("content", content);
            requestBody.put("version", 2);
            requestBody.put("scene", 1);
            requestBody.put("openid", openid);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                int errcode = jsonNode.path("errcode").asInt(0);

                if (errcode == 0) {
                    return true;
                } else if (errcode == 87014) {
                    log.warn("文本内容违规: {}", content);
                    return false;
                } else {
                    log.error("文本检测失败: {}", response.body());
                    return true;
                }
            } else {
                log.error("文本检测HTTP错误: {}", response.statusCode());
                return true;
            }
        } catch (Exception e) {
            log.error("文本内容检测异常", e);
            return true;
        }
    }

    /**
     * 图片内容安全检测
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

        try {
            String url = "https://api.weixin.qq.com/wxa/img_sec_check?access_token=" + token;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("media_url", imageUrl);
            requestBody.put("media_type", 2);
            requestBody.put("version", 2);
            requestBody.put("scene", 1);
            requestBody.put("openid", openid);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                int errcode = jsonNode.path("errcode").asInt(0);

                if (errcode == 0) {
                    return true;
                } else if (errcode == 87014) {
                    log.warn("图片内容违规: {}", imageUrl);
                    return false;
                } else {
                    log.error("图片检测失败: {}", response.body());
                    return true;
                }
            } else {
                log.error("图片检测HTTP错误: {}", response.statusCode());
                return true;
            }
        } catch (Exception e) {
            log.error("图片内容检测异常", e);
            return true;
        }
    }
}
