package com.example.top_hog_server.model; // 请替换为您的实际包路径

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Lombok: 自动生成 getter, setter, toString, equals, hashCode
@NoArgsConstructor // Lombok: 无参构造
@AllArgsConstructor // Lombok: 全参构造
public class ChatMessage {
    private String sender;    // 发送者昵称
    private String text;      // 消息内容
    private Long timestamp;   // 消息时间戳 (可选, 可由服务器设置)
    private String type;      // 消息类型, 例如 "CHAT", "SYSTEM" (可选)
}