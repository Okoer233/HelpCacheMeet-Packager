package com.aeterhilrin.helpcachemeetpackager.model;

import java.util.UUID;

/**
 * 下载任务模型
 * 表示单个文件的下载任务状态和信息
 * 
 * @author AeterHilrin
 */
public class DownloadTask {
    private String taskId;                // 任务ID
    private String originalUrl;           // 原始蓝奏云链接
    private String directUrl;             // 解析后的直链
    private String fileName;              // 文件名
    private String prefix;                // 前缀
    private int suffix;                   // 后缀
    private TaskStatus status;            // 任务状态
    private long fileSize;                // 文件大小
    private long downloadedSize;          // 已下载大小
    private String password;              // 链接密码
    private String errorMessage;          // 错误信息
    private long startTime;               // 开始时间
    private long endTime;                 // 结束时间
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING("等待中"),
        PARSING_URL("解析链接中"),
        DOWNLOADING("下载中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消");
        
        private final String description;
        
        TaskStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public DownloadTask() {
        this.taskId = UUID.randomUUID().toString().replace("-", "");
        this.status = TaskStatus.PENDING;
        this.startTime = System.currentTimeMillis();
    }
    
    public DownloadTask(String originalUrl, String prefix, int suffix, String password) {
        this();
        this.originalUrl = originalUrl;
        this.prefix = prefix;
        this.suffix = suffix;
        this.password = password;
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getOriginalUrl() {
        return originalUrl;
    }
    
    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }
    
    public String getDirectUrl() {
        return directUrl;
    }
    
    public void setDirectUrl(String directUrl) {
        this.directUrl = directUrl;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public int getSuffix() {
        return suffix;
    }
    
    public void setSuffix(int suffix) {
        this.suffix = suffix;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
        if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.CANCELLED) {
            this.endTime = System.currentTimeMillis();
        }
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public long getDownloadedSize() {
        return downloadedSize;
    }
    
    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    /**
     * 获取下载进度百分比
     * @return 下载进度（0-100）
     */
    public double getProgress() {
        if (fileSize <= 0) {
            return 0.0;
        }
        return Math.min(100.0, (double) downloadedSize / fileSize * 100.0);
    }
    
    /**
     * 获取格式化的文件名（包含前缀和后缀）
     * @return 格式化的文件名
     */
    public String getFormattedFileName() {
        if (fileName == null) {
            return null;
        }
        
        return String.format("[%s]%s[%d]", prefix, fileName, suffix);
    }
    
    /**
     * 获取下载速度（字节/秒）
     * @return 下载速度
     */
    public long getDownloadSpeed() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        
        if (elapsedTime <= 0) {
            return 0;
        }
        
        return downloadedSize * 1000 / elapsedTime; // 字节/秒
    }
    
    /**
     * 获取预估剩余时间（毫秒）
     * @return 预估剩余时间
     */
    public long getEstimatedRemainingTime() {
        long speed = getDownloadSpeed();
        if (speed <= 0 || fileSize <= 0) {
            return -1; // 无法估算
        }
        
        long remainingBytes = fileSize - downloadedSize;
        return remainingBytes * 1000 / speed;
    }
    
    /**
     * 检查任务是否已完成（成功或失败）
     * @return 任务是否已完成
     */
    public boolean isFinished() {
        return status == TaskStatus.COMPLETED || 
               status == TaskStatus.FAILED || 
               status == TaskStatus.CANCELLED;
    }
    
    /**
     * 检查任务是否成功完成
     * @return 任务是否成功完成
     */
    public boolean isSuccessful() {
        return status == TaskStatus.COMPLETED;
    }
    
    @Override
    public String toString() {
        return "DownloadTask{" +
                "taskId='" + taskId + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", fileName='" + fileName + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix=" + suffix +
                ", status=" + status +
                ", progress=" + String.format("%.2f", getProgress()) + "%" +
                '}';
    }
}