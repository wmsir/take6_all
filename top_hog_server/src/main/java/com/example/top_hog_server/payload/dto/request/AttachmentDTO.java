package com.example.top_hog_server.payload.dto.request;

/**
 * @author : wangmeng
 * @date 2025/5/24 23:18
 * @description
 */
public class AttachmentDTO { // 可单独文件或同文件定义
    private String id; // 可选，更新时使用
    private String fileName;
    private String url; // 文件访问URL
    private String mimeType;
    private Long size; // 文件大小 (字节)

    public AttachmentDTO() {}

    public AttachmentDTO(String id, String fileName, String url, String mimeType, Long size) {
        this.id = id;
        this.fileName = fileName;
        this.url = url;
        this.mimeType = mimeType;
        this.size = size;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}