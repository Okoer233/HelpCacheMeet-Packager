package com.aeterhilrin.helpcachemeetpackager.model;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件信息模型
 * 用于存储下载完成后的文件信息
 * 
 * @author AeterHilrin
 */
public class FileInfo {
    private String originalFileName;      // 原始文件名
    private String formattedFileName;     // 格式化后的文件名
    private String filePath;              // 文件路径
    private long fileSize;                // 文件大小
    private String prefix;                // 前缀
    private int suffix;                   // 后缀
    private LocalDateTime downloadTime;   // 下载时间
    private boolean selected;             // 是否被选中用于打包
    private String taskId;                // 关联的任务ID
    
    public FileInfo() {
        this.downloadTime = LocalDateTime.now();
        this.selected = false;
    }
    
    public FileInfo(String originalFileName, String filePath, String prefix, int suffix) {
        this();
        this.originalFileName = originalFileName;
        this.filePath = filePath;
        this.prefix = prefix;
        this.suffix = suffix;
        this.formattedFileName = generateFormattedFileName();
        
        // 获取文件大小
        File file = new File(filePath);
        if (file.exists()) {
            this.fileSize = file.length();
        }
    }
    
    /**
     * 生成格式化的文件名
     * @return 格式化的文件名
     */
    private String generateFormattedFileName() {
        if (originalFileName == null) {
            return null;
        }
        return String.format("[%s]%s[%d]", prefix, originalFileName, suffix);
    }
    
    // Getters and Setters
    public String getOriginalFileName() {
        return originalFileName;
    }
    
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
        this.formattedFileName = generateFormattedFileName();
    }
    
    public String getFormattedFileName() {
        return formattedFileName;
    }
    
    public void setFormattedFileName(String formattedFileName) {
        this.formattedFileName = formattedFileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
        
        // 更新文件大小
        File file = new File(filePath);
        if (file.exists()) {
            this.fileSize = file.length();
        }
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        this.formattedFileName = generateFormattedFileName();
    }
    
    public int getSuffix() {
        return suffix;
    }
    
    public void setSuffix(int suffix) {
        this.suffix = suffix;
        this.formattedFileName = generateFormattedFileName();
    }
    
    public LocalDateTime getDownloadTime() {
        return downloadTime;
    }
    
    public void setDownloadTime(LocalDateTime downloadTime) {
        this.downloadTime = downloadTime;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    /**
     * 获取格式化的文件大小
     * @return 格式化的文件大小字符串
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 获取格式化的下载时间
     * @return 格式化的下载时间字符串
     */
    public String getFormattedDownloadTime() {
        if (downloadTime == null) {
            return "";
        }
        return downloadTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * 检查文件是否存在
     * @return 文件是否存在
     */
    public boolean exists() {
        if (filePath == null) {
            return false;
        }
        return new File(filePath).exists();
    }
    
    /**
     * 检查文件是否为ZIP文件
     * @return 是否为ZIP文件
     */
    public boolean isZipFile() {
        if (originalFileName == null) {
            return false;
        }
        return originalFileName.toLowerCase().endsWith(".zip");
    }
    
    /**
     * 获取文件扩展名
     * @return 文件扩展名
     */
    public String getFileExtension() {
        if (originalFileName == null) {
            return "";
        }
        
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalFileName.length() - 1) {
            return originalFileName.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "";
    }
    
    /**
     * 获取不含扩展名的文件名
     * @return 不含扩展名的文件名
     */
    public String getFileNameWithoutExtension() {
        if (originalFileName == null) {
            return "";
        }
        
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return originalFileName.substring(0, lastDotIndex);
        }
        
        return originalFileName;
    }
    
    @Override
    public String toString() {
        return "FileInfo{" +
                "originalFileName='" + originalFileName + '\'' +
                ", formattedFileName='" + formattedFileName + '\'' +
                ", fileSize=" + getFormattedFileSize() +
                ", prefix='" + prefix + '\'' +
                ", suffix=" + suffix +
                ", selected=" + selected +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        FileInfo fileInfo = (FileInfo) o;
        
        if (suffix != fileInfo.suffix) return false;
        if (prefix != null ? !prefix.equals(fileInfo.prefix) : fileInfo.prefix != null) return false;
        return originalFileName != null ? originalFileName.equals(fileInfo.originalFileName) : fileInfo.originalFileName == null;
    }
    
    @Override
    public int hashCode() {
        int result = originalFileName != null ? originalFileName.hashCode() : 0;
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + suffix;
        return result;
    }
}