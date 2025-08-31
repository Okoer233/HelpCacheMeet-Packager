package com.aeterhilrin.helpcachemeetpackager.exception;

/**
 * 应用程序基础异常类
 * 
 * @author AeterHilrin
 */
public class HelpCacheMeetPackagerException extends Exception {
    
    public HelpCacheMeetPackagerException() {
        super();
    }
    
    public HelpCacheMeetPackagerException(String message) {
        super(message);
    }
    
    public HelpCacheMeetPackagerException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public HelpCacheMeetPackagerException(Throwable cause) {
        super(cause);
    }
}

/**
 * 配置文件异常
 */
class InvalidConfigException extends HelpCacheMeetPackagerException {
    
    public InvalidConfigException(String message) {
        super("配置文件错误: " + message);
    }
    
    public InvalidConfigException(String message, Throwable cause) {
        super("配置文件错误: " + message, cause);
    }
}

/**
 * 缺少项目名称异常
 */
class MissingProjectNameException extends InvalidConfigException {
    
    public MissingProjectNameException() {
        super("配置文件中缺少项目名称");
    }
}

/**
 * API连接异常
 */
class ApiConnectionException extends HelpCacheMeetPackagerException {
    
    public ApiConnectionException(String message) {
        super("API连接失败: " + message);
    }
    
    public ApiConnectionException(String message, Throwable cause) {
        super("API连接失败: " + message, cause);
    }
}

/**
 * 下载异常
 */
class DownloadException extends HelpCacheMeetPackagerException {
    
    public DownloadException(String message) {
        super("下载失败: " + message);
    }
    
    public DownloadException(String message, Throwable cause) {
        super("下载失败: " + message, cause);
    }
}

/**
 * 文件访问异常
 */
class FileAccessException extends HelpCacheMeetPackagerException {
    
    public FileAccessException(String message) {
        super("文件访问失败: " + message);
    }
    
    public FileAccessException(String message, Throwable cause) {
        super("文件访问失败: " + message, cause);
    }
}

/**
 * 无效链接异常
 */
class InvalidUrlException extends HelpCacheMeetPackagerException {
    
    public InvalidUrlException(String url) {
        super("无效的蓝奏云链接: " + url);
    }
    
    public InvalidUrlException(String url, Throwable cause) {
        super("无效的蓝奏云链接: " + url, cause);
    }
}

/**
 * 打包异常
 */
class PackageException extends HelpCacheMeetPackagerException {
    
    public PackageException(String message) {
        super("打包操作失败: " + message);
    }
    
    public PackageException(String message, Throwable cause) {
        super("打包操作失败: " + message, cause);
    }
}