package com.example.top_hog_server.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

/**
 * Jasypt加密工具类
 * 用于生成加密后的配置值
 */
public class JasyptEncryptorUtil {

    public static void main(String[] args) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();

        // 设置加密密钥(与application.properties中的jasypt.encryptor.password一致)
        String password = "TopHogSecretKey2024"; // 实际使用时应从环境变量读取
        encryptor.setPassword(password);
        encryptor.setAlgorithm("PBEWithMD5AndDES");

        // 需要加密的敏感信息
        String dbPassword = "f5mF2hKiOkbxKqs5";
        String wechatSecret = "7ff4fae3ca994f587c5d27fcd579887b";
        String mailPassword = "wgyypgcgqflubhji";
        String jwtSecret = "mN+mJRYlGtjflZzTQFekx1i7LNSC5nJ3L9jsgiDSbIagJ+veHko04XcGefgGJ5fbd1mIzlmLJp93zwoFVzi3AA==";

        // 加密
        System.out.println("=== 加密结果 ===");
        System.out.println("数据库密码: ENC(" + encryptor.encrypt(dbPassword) + ")");
        System.out.println("微信AppSecret: ENC(" + encryptor.encrypt(wechatSecret) + ")");
        System.out.println("邮箱密码: ENC(" + encryptor.encrypt(mailPassword) + ")");
        System.out.println("JWT密钥: ENC(" + encryptor.encrypt(jwtSecret) + ")");

        // 验证解密
        System.out.println("\n=== 验证解密 ===");
        String encryptedDbPassword = encryptor.encrypt(dbPassword);
        String decryptedDbPassword = encryptor.decrypt(encryptedDbPassword);
        System.out.println("解密后的数据库密码: " + decryptedDbPassword);
        System.out.println("解密是否正确: " + dbPassword.equals(decryptedDbPassword));
    }
}
