package com.example.take6server.payload.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    @Size(max = 50)
    private String nickname;

    @Size(max = 255)
    private String avatarUrl;

    @Size(max = 20)
    private String phone;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Size(max = 100)
    private String wechatOpenid;

    @Size(max = 100)
    private String qqOpenid;

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
}
