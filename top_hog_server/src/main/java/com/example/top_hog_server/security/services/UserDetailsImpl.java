package com.example.top_hog_server.security.services;

import com.example.top_hog_server.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;

public class UserDetailsImpl implements UserDetails {
    
    private static final long serialVersionUID = 2375620020645199359L;

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String inviteCode;
    private String wechatOpenid;
    private String qqOpenid;
    private Integer vipStatus;
    private Long tenantId;

    @JsonIgnore
    private String password;

    public UserDetailsImpl(Long id, String username, String email, String password, 
                          String nickname,String qqOpenid, String wechatOpenid,  String avatarUrl, String phone,
                          String inviteCode, Integer vipStatus, Long tenantId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.qqOpenid = qqOpenid;
        this.wechatOpenid = wechatOpenid;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
        this.inviteCode = inviteCode;
        this.vipStatus = vipStatus;
        this.tenantId = tenantId;
    }



    public static UserDetailsImpl build(User user) {
        // 如果 username 为空（例如微信登录用户），尝试使用 openid 作为 username
        String principalUsername = user.getUsername();
        if (principalUsername == null || principalUsername.isEmpty()) {
            if (user.getWechatOpenid() != null && !user.getWechatOpenid().isEmpty()) {
                principalUsername = user.getWechatOpenid();
            } else if (user.getQqOpenid() != null && !user.getQqOpenid().isEmpty()) {
                principalUsername = user.getQqOpenid();
            }
        }

        return new UserDetailsImpl(
                user.getId(),
                principalUsername, // 使用处理后的 principalUsername
                user.getEmail(),
                user.getPassword(),
                user.getNickname(),
                user.getQqOpenid(),
                user.getWechatOpenid(),
                user.getAvatarUrl(),
                user.getPhone(),
                user.getInviteCode(),
                user.getVipStatus(),
                user.getTenantId());
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getInviteCode() {
        return inviteCode;
    }
    
    public Integer getVipStatus() {
        return vipStatus;
    }

    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getWechatOpenid() {
        return wechatOpenid;
    }

    public void setWechatOpenid(String wechatOpenid) {
        this.wechatOpenid = wechatOpenid;
    }

    public String getQqOpenid() {
        return qqOpenid;
    }

    public void setQqOpenid(String qqOpenid) {
        this.qqOpenid = qqOpenid;
    }

    public void setVipStatus(Integer vipStatus) {
        this.vipStatus = vipStatus;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}
