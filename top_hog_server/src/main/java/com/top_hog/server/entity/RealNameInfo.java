package com.top_hog.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 实名认证信息实体
 */
@Data
@Entity
@Table(name = "real_name_info")
@EntityListeners(AuditingEntityListener.class)
public class RealNameInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    // 真实姓名 (敏感信息，实际生产应加密存储)
    @Column(nullable = false)
    private String realName;

    // 身份证号 (敏感信息，实际生产应加密存储)
    @Column(nullable = false)
    private String idCard;

    // 是否已认证
    private Boolean isVerified = false;

    // 年龄 (根据身份证自动计算)
    private Integer age;

    // 是否成年
    private Boolean isAdult = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
