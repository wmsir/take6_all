package com.example.top_hog_server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * JPA 实体类，映射到数据库中的 'Users' 表。
 * 存储用户的账户和基本信息。
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 修改：允许为 null (nullable = true)
    // 微信登录时没有用户名，只有 openid
    @Column(name = "username", unique = true, length = 50, nullable = true)
    private String username;

    // 修改：允许为 null (nullable = true)
    // 微信登录没法直接获取邮箱
    @Column(name = "email", unique = true, length = 100, nullable = true)
    private String email;

    // 修改：允许为 null (nullable = true)
    // 微信登录不需要密码
    @Column(name = "password", length = 120, nullable = true)
    private String password;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "wechat_openid", length = 100)
    private String wechatOpenid;

    @Column(name = "wechat_session_key", length = 100)
    private String wechatSessionKey;

    @Column(name = "qq_openid", length = 100)
    private String qqOpenid;

    @Column(name = "invite_code", unique = true, length = 10)
    private String inviteCode;

    // 注意：int 是基本类型，默认是 0，不会为 null，所以数据库里会存为 0，不会报错
    @Column(name = "vip_status")
    private int vipStatus;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "created_at", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp updatedAt;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    public Long getRegisterTime() {
        return createdAt != null ? createdAt.getTime() : null;
    }

    // 保留这个构造函数给传统的账号密码注册使用
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.nickname = username;
    }

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}