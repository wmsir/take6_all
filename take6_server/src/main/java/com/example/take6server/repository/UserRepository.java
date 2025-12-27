package com.example.take6server.repository;

import com.example.take6server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 实体的 JPA Repository 接口。
 * 提供对 Users 表的 CRUD 操作以及自定义查询。
 * User 的主键类型是 Long (id)。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户。
     * @param username 用户名
     * @return 包含用户（如果找到）的 Optional 对象
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查指定用户名的用户是否存在。
     * @param username 要检查的用户名
     * @return 如果用户存在返回true，否则返回false
     */
    Boolean existsByUsername(String username);

    /**
     * 检查指定邮箱的用户是否存在。
     * @param email 要检查的邮箱地址
     * @return 如果用户存在返回true，否则返回false
     */
    Boolean existsByEmail(String email);

    /**
     * 根据邮箱地址查找用户。
     * @param email 要查找的邮箱地址
     * @return 包含用户的Optional对象，如果未找到则为空
     */
    Optional<User> findByEmail(String email);


    // 【重要修改】方法名必须匹配 User.java 中的属性名 wechatOpenid
    // Spring Data JPA 会自动解析为 SQL: select * from users where wechat_openid = ?
    Optional<User> findByWechatOpenid(String wechatOpenid);
}