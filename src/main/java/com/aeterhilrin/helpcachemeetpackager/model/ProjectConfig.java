package com.aeterhilrin.helpcachemeetpackager.model;

import java.util.List;
import java.util.ArrayList;

/**
 * 项目配置数据模型
 * 用于存储从YAML配置文件解析出的项目信息
 * 
 * @author AeterHilrin
 */
public class ProjectConfig {
    private String projectName;           // 项目名称
    private List<DownloadItem> items;     // 下载项目列表
    
    public ProjectConfig() {
        this.items = new ArrayList<>();
    }
    
    public ProjectConfig(String projectName) {
        this.projectName = projectName;
        this.items = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public List<DownloadItem> getItems() {
        return items;
    }
    
    public void setItems(List<DownloadItem> items) {
        this.items = items;
    }
    
    public void addItem(DownloadItem item) {
        this.items.add(item);
    }
    
    /**
     * 验证配置是否有效
     * @return 配置是否有效
     */
    public boolean isValid() {
        if (projectName == null || projectName.trim().isEmpty()) {
            return false;
        }
        
        if (items == null || items.isEmpty()) {
            return false;
        }
        
        for (DownloadItem item : items) {
            if (!item.isValid()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return "ProjectConfig{" +
                "projectName='" + projectName + '\'' +
                ", items=" + items +
                '}';
    }
    
    /**
     * 下载项目数据模型
     * 包含单个下载任务的所有信息
     */
    public static class DownloadItem {
        private String prefix;            // 前缀
        private String lanzouUrl;         // 蓝奏云链接
        private String password;          // 链接密码
        private int suffix;               // 后缀（数字）
        
        public DownloadItem() {
        }
        
        public DownloadItem(String prefix, String lanzouUrl, String password, int suffix) {
            this.prefix = prefix;
            this.lanzouUrl = lanzouUrl;
            this.password = password;
            this.suffix = suffix;
        }
        
        // Getters and Setters
        public String getPrefix() {
            return prefix;
        }
        
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
        
        public String getLanzouUrl() {
            return lanzouUrl;
        }
        
        public void setLanzouUrl(String lanzouUrl) {
            this.lanzouUrl = lanzouUrl;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public int getSuffix() {
            return suffix;
        }
        
        public void setSuffix(int suffix) {
            this.suffix = suffix;
        }
        
        /**
         * 验证下载项是否有效
         * @return 下载项是否有效
         */
        public boolean isValid() {
            if (prefix == null || prefix.trim().isEmpty()) {
                return false;
            }
            
            if (lanzouUrl == null || lanzouUrl.trim().isEmpty()) {
                return false;
            }
            
            // 验证蓝奏云链接格式
            if (!lanzouUrl.contains("lanzou") && !lanzouUrl.contains("lanzoui") 
                && !lanzouUrl.contains("lanzous") && !lanzouUrl.contains("lanzouo")) {
                return false;
            }
            
            return suffix >= 0;
        }
        
        /**
         * 检查是否有密码
         * @return 是否有密码
         */
        public boolean hasPassword() {
            return password != null && !password.trim().isEmpty() && !"无".equals(password.trim());
        }
        
        @Override
        public String toString() {
            return "DownloadItem{" +
                    "prefix='" + prefix + '\'' +
                    ", lanzouUrl='" + lanzouUrl + '\'' +
                    ", password='" + password + '\'' +
                    ", suffix=" + suffix +
                    '}';
        }
    }
}