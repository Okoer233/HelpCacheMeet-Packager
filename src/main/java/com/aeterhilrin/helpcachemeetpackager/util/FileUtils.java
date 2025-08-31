package com.aeterhilrin.helpcachemeetpackager.util;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 工具类
 * 提供各种常用的工具方法
 * 
 * @author AeterHilrin
 */
public class FileUtils {
    
    private static final String[] SIZE_UNITS = {"B", "KB", "MB", "GB", "TB"};
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化的大小字符串
     */
    public static String formatFileSize(long size) {
        if (size <= 0) {
            return "0 B";
        }
        
        int unitIndex = 0;
        double fileSize = size;
        
        while (fileSize >= 1024 && unitIndex < SIZE_UNITS.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }
        
        if (unitIndex == 0) {
            return String.format("%.0f %s", fileSize, SIZE_UNITS[unitIndex]);
        } else {
            return String.format("%.2f %s", fileSize, SIZE_UNITS[unitIndex]);
        }
    }
    
    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 扩展名（不包含点），没有扩展名返回空字符串
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "";
    }
    
    /**
     * 获取不含扩展名的文件名
     * @param fileName 文件名
     * @return 不含扩展名的文件名
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        
        return fileName;
    }
    
    /**
     * 检查文件是否为图片
     * @param fileName 文件名
     * @return 是否为图片文件
     */
    public static boolean isImageFile(String fileName) {
        String extension = getFileExtension(fileName);
        Set<String> imageExtensions = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
        return imageExtensions.contains(extension);
    }
    
    /**
     * 检查文件是否为压缩文件
     * @param fileName 文件名
     * @return 是否为压缩文件
     */
    public static boolean isArchiveFile(String fileName) {
        String extension = getFileExtension(fileName);
        Set<String> archiveExtensions = Set.of("zip", "rar", "7z", "tar", "gz", "bz2", "xz");
        return archiveExtensions.contains(extension);
    }
    
    /**
     * 安全地删除文件或目录
     * @param path 文件或目录路径
     * @return 删除是否成功
     */
    public static boolean safeDelete(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        try {
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                if (Files.isDirectory(filePath)) {
                    // 递归删除目录
                    Files.walk(filePath)
                         .sorted(Comparator.reverseOrder())
                         .map(Path::toFile)
                         .forEach(File::delete);
                } else {
                    // 删除文件
                    Files.delete(filePath);
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("删除文件失败: " + path + " - " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 复制文件
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 复制是否成功
     */
    public static boolean copyFile(String sourcePath, String targetPath) {
        if (sourcePath == null || targetPath == null) {
            return false;
        }
        
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);
            
            // 创建目标目录
            Files.createDirectories(target.getParent());
            
            // 复制文件
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
            
        } catch (Exception e) {
            System.err.println("复制文件失败: " + sourcePath + " -> " + targetPath + " - " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 计算文件MD5哈希值
     * @param filePath 文件路径
     * @return MD5哈希值，失败返回null
     */
    public static String calculateMD5(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            System.err.println("计算MD5失败: " + filePath + " - " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 读取文件内容为字符串
     * @param filePath 文件路径
     * @param charset 字符编码
     * @return 文件内容，失败返回null
     */
    public static String readFileToString(String filePath, String charset) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            return new String(bytes, charset != null ? charset : "UTF-8");
        } catch (Exception e) {
            System.err.println("读取文件失败: " + filePath + " - " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 写入字符串到文件
     * @param content 文件内容
     * @param filePath 文件路径
     * @param charset 字符编码
     * @return 写入是否成功
     */
    public static boolean writeStringToFile(String content, String filePath, String charset) {
        if (content == null || filePath == null) {
            return false;
        }
        
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            
            byte[] bytes = content.getBytes(charset != null ? charset : "UTF-8");
            Files.write(path, bytes);
            return true;
            
        } catch (Exception e) {
            System.err.println("写入文件失败: " + filePath + " - " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 格式化时间戳
     * @param timestamp 时间戳（毫秒）
     * @return 格式化的时间字符串
     */
    public static String formatTimestamp(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    /**
     * 获取当前时间戳字符串
     * @return 当前时间戳字符串
     */
    public static String getCurrentTimestamp() {
        return formatTimestamp(System.currentTimeMillis());
    }
    
    /**
     * 创建目录（如果不存在）
     * @param dirPath 目录路径
     * @return 创建是否成功
     */
    public static boolean ensureDirectoryExists(String dirPath) {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            return false;
        }
        
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return Files.exists(path) && Files.isDirectory(path);
        } catch (Exception e) {
            System.err.println("创建目录失败: " + dirPath + " - " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 获取临时目录路径
     * @return 临时目录路径
     */
    public static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir");
    }
    
    /**
     * 生成唯一的临时文件名
     * @param prefix 前缀
     * @param suffix 后缀（扩展名）
     * @return 临时文件名
     */
    public static String generateTempFileName(String prefix, String suffix) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf(new Random().nextInt(10000));
        
        return (prefix != null ? prefix : "temp") + "_" + timestamp + "_" + random + 
               (suffix != null ? suffix : ".tmp");
    }
    
    /**
     * 检查网络连接
     * @param host 主机地址
     * @param timeout 超时时间（毫秒）
     * @return 是否可连接
     */
    public static boolean isNetworkReachable(String host, int timeout) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(timeout);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取本机IP地址
     * @return IP地址列表
     */
    public static List<String> getLocalIPAddresses() {
        List<String> ipAddresses = new ArrayList<>();
        
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.isSiteLocalAddress()) {
                        ipAddresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        
        return ipAddresses;
    }
    
    /**
     * 验证文件路径格式
     * @param path 文件路径
     * @return 是否为有效路径
     */
    public static boolean isValidPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        try {
            Paths.get(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 清理路径字符串
     * @param path 原始路径
     * @return 清理后的路径
     */
    public static String cleanPath(String path) {
        if (path == null) {
            return null;
        }
        
        // 标准化路径分隔符
        String cleanedPath = path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
        
        // 移除重复的路径分隔符
        String separator = Pattern.quote(File.separator);
        cleanedPath = cleanedPath.replaceAll(separator + "+", File.separator);
        
        return cleanedPath;
    }
    
    /**
     * 获取系统信息
     * @return 系统信息映射
     */
    public static Map<String, String> getSystemInfo() {
        Map<String, String> systemInfo = new HashMap<>();
        
        systemInfo.put("操作系统", System.getProperty("os.name"));
        systemInfo.put("系统版本", System.getProperty("os.version"));
        systemInfo.put("系统架构", System.getProperty("os.arch"));
        systemInfo.put("Java版本", System.getProperty("java.version"));
        systemInfo.put("Java供应商", System.getProperty("java.vendor"));
        systemInfo.put("用户目录", System.getProperty("user.home"));
        systemInfo.put("工作目录", System.getProperty("user.dir"));
        systemInfo.put("临时目录", System.getProperty("java.io.tmpdir"));
        
        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        systemInfo.put("最大内存", formatFileSize(maxMemory));
        systemInfo.put("总内存", formatFileSize(totalMemory));
        systemInfo.put("已用内存", formatFileSize(usedMemory));
        systemInfo.put("可用内存", formatFileSize(freeMemory));
        
        return systemInfo;
    }
}