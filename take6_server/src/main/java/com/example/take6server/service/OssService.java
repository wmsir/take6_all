package com.example.take6server.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.example.take6server.config.OssConfig;
import com.example.take6server.exception.BusinessException;
import com.example.take6server.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class OssService {

    private static final Logger logger = LoggerFactory.getLogger(OssService.class);

    @Autowired
    private OssConfig ossConfig;

    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg"; // Default extension

        // Generate unique filename
        String fileName = "avatars/" + UUID.randomUUID().toString().replace("-", "") + suffix;

        OSS ossClient = new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
        );

        try {
            ossClient.putObject(ossConfig.getBucketName(), fileName, file.getInputStream());
        } catch (IOException e) {
            logger.error("Failed to upload file to OSS", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "File upload failed");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // Return URL
        // If urlPrefix is set, use it. Otherwise construct from endpoint and bucket.
        // Endpoint format: https://oss-cn-hangzhou.aliyuncs.com
        // Bucket URL format: https://bucket-name.oss-cn-hangzhou.aliyuncs.com/filename

        String url;
        if (ossConfig.getUrlPrefix() != null && !ossConfig.getUrlPrefix().isEmpty()) {
            url = ossConfig.getUrlPrefix() + "/" + fileName;
        } else {
            String endpoint = ossConfig.getEndpoint();
            // Basic handling to insert bucket name into domain if needed, or assume standard format
            // Standard: https://{bucket}.{endpoint-without-protocol}/{key}
            // Or just: https://{bucket}.{endpoint}/{key} if endpoint is just hostname?
            // Usually endpoint in config includes https://.

            // Let's assume endpoint is like "oss-cn-hangzhou.aliyuncs.com" or "https://oss-cn-hangzhou.aliyuncs.com"
            // For simplicity, let's construct standard public URL
            String protocol = "https://";
            String domain = endpoint;
            if (endpoint.startsWith("http://")) {
                protocol = "http://";
                domain = endpoint.substring(7);
            } else if (endpoint.startsWith("https://")) {
                protocol = "https://";
                domain = endpoint.substring(8);
            }

            url = protocol + ossConfig.getBucketName() + "." + domain + "/" + fileName;
        }

        return url;
    }
}
