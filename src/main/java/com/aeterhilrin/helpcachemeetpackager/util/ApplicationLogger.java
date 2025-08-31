package com.aeterhilrin.helpcachemeetpackager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用程序日志记录器
 * 提供统一的日志记录接口
 * 
 * @author AeterHilrin
 */
public class ApplicationLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationLogger.class);
    
    /**
     * 记录应用程序启动
     */
    public static void logApplicationStart() {
        logger.info("=== HelpCacheMeetPackager 应用程序启动 ===");
        logger.info("启动时间: {}", FileUtils.getCurrentTimestamp());
        
        // 记录系统信息
        logger.info("系统信息:");
        FileUtils.getSystemInfo().forEach((key, value) -> {
            logger.info("  {}: {}", key, value);
        });
    }
    
    /**
     * 记录应用程序关闭
     */
    public static void logApplicationShutdown() {
        logger.info("=== HelpCacheMeetPackager 应用程序关闭 ===");
        logger.info("关闭时间: {}", FileUtils.getCurrentTimestamp());
    }
    
    /**
     * 记录配置文件加载
     */
    public static void logConfigLoaded(String filePath, String projectName, int itemCount) {
        logger.info("配置文件加载成功 - 文件: {}, 项目: {}, 下载项: {}", filePath, projectName, itemCount);
    }
    
    /**
     * 记录配置文件加载失败
     */
    public static void logConfigLoadFailed(String filePath, String errorMessage) {
        logger.error("配置文件加载失败 - 文件: {}, 错误: {}", filePath, errorMessage);
    }
    
    /**
     * 记录下载任务开始
     */
    public static void logDownloadStart(String url, String fileName) {
        logger.info("开始下载文件 - URL: {}, 文件名: {}", url, fileName);
    }
    
    /**
     * 记录下载进度
     */
    public static void logDownloadProgress(String fileName, long downloaded, long total) {
        if (total > 0) {
            double progress = (double) downloaded / total * 100;
            logger.debug("下载进度 - 文件: {}, 进度: {:.2f}% ({}/{})", 
                fileName, progress, FileUtils.formatFileSize(downloaded), FileUtils.formatFileSize(total));
        } else {
            logger.debug("下载进度 - 文件: {}, 已下载: {}", fileName, FileUtils.formatFileSize(downloaded));
        }
    }
    
    /**
     * 记录下载完成
     */
    public static void logDownloadCompleted(String fileName, long fileSize, long duration) {
        logger.info("下载完成 - 文件: {}, 大小: {}, 耗时: {}ms", 
            fileName, FileUtils.formatFileSize(fileSize), duration);
    }
    
    /**
     * 记录下载失败
     */
    public static void logDownloadFailed(String fileName, String errorMessage) {
        logger.error("下载失败 - 文件: {}, 错误: {}", fileName, errorMessage);
    }
    
    /**
     * 记录文件重命名
     */
    public static void logFileRenamed(String oldName, String newName) {
        logger.info("文件重命名 - 原名: {}, 新名: {}", oldName, newName);
    }
    
    /**
     * 记录文件重命名失败
     */
    public static void logFileRenameFailed(String fileName, String errorMessage) {
        logger.error("文件重命名失败 - 文件: {}, 错误: {}", fileName, errorMessage);
    }
    
    /**
     * 记录ZIP解压开始
     */
    public static void logExtractionStart(String zipFile, String outputDir) {
        logger.info("开始解压文件 - ZIP: {}, 输出目录: {}", zipFile, outputDir);
    }
    
    /**
     * 记录ZIP解压完成
     */
    public static void logExtractionCompleted(String zipFile, int fileCount, long duration) {
        logger.info("解压完成 - ZIP: {}, 文件数: {}, 耗时: {}ms", zipFile, fileCount, duration);
    }
    
    /**
     * 记录ZIP解压失败
     */
    public static void logExtractionFailed(String zipFile, String errorMessage) {
        logger.error("解压失败 - ZIP: {}, 错误: {}", zipFile, errorMessage);
    }
    
    /**
     * 记录文件冲突处理
     */
    public static void logFileConflict(String fileName, String action) {
        logger.warn("文件冲突 - 文件: {}, 处理方式: {}", fileName, action);
    }
    
    /**
     * 记录打包操作开始
     */
    public static void logPackageStart(String projectName, int fileCount) {
        logger.info("开始打包项目 - 项目: {}, 文件数: {}", projectName, fileCount);
    }
    
    /**
     * 记录打包操作完成
     */
    public static void logPackageCompleted(String projectName, String outputPath, int processedFiles, long duration) {
        logger.info("打包完成 - 项目: {}, 输出: {}, 处理文件: {}, 耗时: {}ms", 
            projectName, outputPath, processedFiles, duration);
    }
    
    /**
     * 记录打包操作失败
     */
    public static void logPackageFailed(String projectName, String errorMessage) {
        logger.error("打包失败 - 项目: {}, 错误: {}", projectName, errorMessage);
    }
    
    /**
     * 记录API调用
     */
    public static void logApiCall(String url, String method, int responseCode) {
        logger.debug("API调用 - URL: {}, 方法: {}, 响应码: {}", url, method, responseCode);
    }
    
    /**
     * 记录API调用失败
     */
    public static void logApiCallFailed(String url, String errorMessage) {
        logger.error("API调用失败 - URL: {}, 错误: {}", url, errorMessage);
    }
    
    /**
     * 记录网络连接测试
     */
    public static void logNetworkTest(String host, boolean reachable) {
        if (reachable) {
            logger.info("网络连接测试成功 - 主机: {}", host);
        } else {
            logger.warn("网络连接测试失败 - 主机: {}", host);
        }
    }
    
    /**
     * 记录临时文件清理
     */
    public static void logTempFileCleanup(int fileCount, boolean success) {
        if (success) {
            logger.info("临时文件清理完成 - 清理文件数: {}", fileCount);
        } else {
            logger.warn("临时文件清理失败 - 预期清理文件数: {}", fileCount);
        }
    }
    
    /**
     * 记录用户操作
     */
    public static void logUserAction(String action, String details) {
        logger.info("用户操作 - 动作: {}, 详情: {}", action, details);
    }
    
    /**
     * 记录异常
     */
    public static void logException(String operation, Exception exception) {
        logger.error("操作异常 - 操作: {}, 异常: {}", operation, exception.getMessage(), exception);
    }
    
    /**
     * 记录性能统计
     */
    public static void logPerformanceStats(String operation, long duration, long memoryUsed) {
        logger.info("性能统计 - 操作: {}, 耗时: {}ms, 内存使用: {}", 
            operation, duration, FileUtils.formatFileSize(memoryUsed));
    }
    
    /**
     * 记录调试信息
     */
    public static void logDebug(String message, Object... args) {
        logger.debug(message, args);
    }
    
    /**
     * 记录信息
     */
    public static void logInfo(String message, Object... args) {
        logger.info(message, args);
    }
    
    /**
     * 记录警告
     */
    public static void logWarn(String message, Object... args) {
        logger.warn(message, args);
    }
    
    /**
     * 记录错误
     */
    public static void logError(String message, Object... args) {
        logger.error(message, args);
    }
    
    /**
     * 记录错误（带异常）
     */
    public static void logError(String message, Throwable throwable, Object... args) {
        logger.error(message, args);
        logger.error("异常详情:", throwable);
    }
    
    /**
     * 获取日志器实例
     */
    public static Logger getLogger() {
        return logger;
    }
    
    /**
     * 获取指定类的日志器
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * 获取指定名称的日志器
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
}