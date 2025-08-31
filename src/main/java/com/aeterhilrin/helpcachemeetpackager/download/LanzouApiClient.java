package com.aeterhilrin.helpcachemeetpackager.download;

import com.aeterhilrin.helpcachemeetpackager.util.ApplicationLogger;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * 蓝奏云API解析
 * 负责解析蓝奏云链接为直链
 * 
 * @author AeterHilrin
 */
public class LanzouApiClient {
    
    private static final String API_BASE_URL = "https://api.ulq.cc/int/v1/lanzou";
    private static final int CONNECT_TIMEOUT = 15000; // 15秒
    private static final int READ_TIMEOUT = 30000; // 30秒
    private static final String USER_AGENT = "HelpCacheMeetPackager/1.0.0";
    
    // 同步锁，防止多线程并发时日志输出混乱
    private static final Object API_LOCK = new Object();
    
    /**
     * API响应结果类
     */
    public static class ApiResponse {
        private final boolean success;
        private final String directUrl;
        private final String fileName;
        private final String errorMessage;
        private final long fileSize;
        
        public ApiResponse(boolean success, String directUrl, String fileName, long fileSize, String errorMessage) {
            this.success = success;
            this.directUrl = directUrl;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public String getDirectUrl() { return directUrl; }
        public String getFileName() { return fileName; }
        public String getErrorMessage() { return errorMessage; }
        public long getFileSize() { return fileSize; }
        
        @Override
        public String toString() {
            return "ApiResponse{" +
                    "success=" + success +
                    ", directUrl='" + directUrl + '\'' +
                    ", fileName='" + fileName + '\'' +
                    ", fileSize=" + fileSize +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }
    
    /**
     * 解析蓝奏云链接为直链
     */
    public static ApiResponse parseDirectUrl(String lanzouUrl) {
        return parseDirectUrl(lanzouUrl, null);
    }
    
    /**
     * 解析蓝奏云链接为直链（带密码）
     * 处理密码参数，当密码为"无"时传递空的pwd参数
     */
    public static ApiResponse parseDirectUrl(String lanzouUrl, String password) {
        if (lanzouUrl == null || lanzouUrl.trim().isEmpty()) {
            return new ApiResponse(false, null, null, 0, "蓝奏云链接不能为空");
        }
        
        // 使用同步锁确保每个下载任务的日志输出是独立和有序的
        synchronized (API_LOCK) {
            ApplicationLogger.logInfo("===== 蓝奏云API调试信息 =====");
            ApplicationLogger.logInfo("线程: {}", Thread.currentThread().getName());
            ApplicationLogger.logInfo("开始解析蓝奏云链接: {}", lanzouUrl);
            ApplicationLogger.logInfo("原始密码参数: {}", password);
            
            try {
                // 构建请求URL - 这是关键修复点
                String requestUrl = buildRequestUrl(lanzouUrl, password);
                ApplicationLogger.logInfo("最终请求URL: {}", requestUrl);
                
                // 发送HTTP请求
                String response = sendHttpRequest(requestUrl);
                ApplicationLogger.logInfo("API响应: {}", response);
                
                // 解析响应
                ApiResponse result = parseApiResponse(response);
                ApplicationLogger.logInfo("解析结果: {}", result.toString());
                ApplicationLogger.logInfo("===== 蓝奏云API调试结束 =====\n");
                
                return result;
                
            } catch (Exception e) {
                ApplicationLogger.logError("API调用异常: {}", e.getMessage());
                ApplicationLogger.logInfo("===== 蓝奏云API调试结束 =====\n");
                return new ApiResponse(false, null, null, 0, "API调用失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 构建请求URL
     * 关键修复：正确处理密码参数
     */
    private static String buildRequestUrl(String lanzouUrl, String password) throws UnsupportedEncodingException {
        StringBuilder urlBuilder = new StringBuilder(API_BASE_URL);
        
        // 编码URL参数
        String encodedUrl = URLEncoder.encode(lanzouUrl, StandardCharsets.UTF_8.name());
        ApplicationLogger.logInfo("编码后的URL: {}", encodedUrl);
        
        // 添加url参数
        urlBuilder.append("?url=").append(encodedUrl);
        
        // 处理密码参数 - 这是关键修复点
        if (password == null || "无".equals(password) || password.trim().isEmpty()) {
            // 当密码为空、null或"无"时，传递空的pwd参数
            urlBuilder.append("&pwd=");
            ApplicationLogger.logInfo("添加空密码参数: &pwd=");
        } else {
            // 有实际密码时，编码并传递
            String encodedPassword = URLEncoder.encode(password.trim(), StandardCharsets.UTF_8.name());
            urlBuilder.append("&pwd=").append(encodedPassword);
            ApplicationLogger.logInfo("添加密码参数: &pwd={}", encodedPassword);
        }
        
        return urlBuilder.toString();
    }
    
    /**
     * 发送HTTP请求
     */
    private static String sendHttpRequest(String requestUrl) throws IOException {
        ApplicationLogger.logInfo("发送HTTP GET请求: {}", requestUrl);
        
        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // 设置请求属性
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            connection.setRequestProperty("Cache-Control", "no-cache");
            
            // 发送请求
            int responseCode = connection.getResponseCode();
            ApplicationLogger.logInfo("HTTP响应码: {}", responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(connection.getInputStream());
                ApplicationLogger.logInfo("成功响应内容: {}", response.substring(0, Math.min(200, response.length())) + "...");
                return response;
            } else {
                String errorResponse = readResponse(connection.getErrorStream());
                ApplicationLogger.logError("错误响应内容: {}", errorResponse);
                throw new IOException("HTTP " + responseCode + ": " + errorResponse);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * 读取响应内容
     */
    private static String readResponse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }
    
    /**
     * 解析API响应
     */
    private static ApiResponse parseApiResponse(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            return new ApiResponse(false, null, null, 0, "API响应为空");
        }
        
        try {
            ApplicationLogger.logInfo("解析JSON响应: {}", responseText);
            JSONObject jsonResponse = new JSONObject(responseText);
            
            // 检查状态码
            int code = jsonResponse.optInt("code", -1);
            ApplicationLogger.logInfo("响应状态码: {}", code);
            
            if (code != 200) {
                String message = jsonResponse.optString("msg", "");
                
                // 如果msg为空或null，检查是否有其他错误信息
                if (message == null || message.trim().isEmpty()) {
                    // 尝试从响应中提取其他可能的错误信息
                    if (responseText.contains("error")) {
                        message = "API服务返回错误状态";
                    } else {
                        message = "API服务暂时不可用，请稍后重试";
                    }
                }
                
                ApplicationLogger.logError("API错误 - 状态码: {}, 消息: {}", code, message);
                return new ApiResponse(false, null, null, 0, "API错误 " + code + ": " + message);
            }
            
            // 获取响应消息
            String msg = jsonResponse.optString("msg", "");
            ApplicationLogger.logInfo("响应消息: {}", msg);
            
            // 解析data字段中的文件信息
            JSONObject dataObj = jsonResponse.optJSONObject("data");
            String fileName = null;
            String fileSize = "0";
            String downUrl = null;
            
            if (dataObj != null) {
                fileName = dataObj.optString("name", null);
                fileSize = dataObj.optString("size", "0");
                downUrl = dataObj.optString("url", null);
                ApplicationLogger.logInfo("从data字段解析 - 文件名: {}, 大小: {}", fileName, fileSize);
            } else {
                ApplicationLogger.logWarn("响应中未找到data字段");
                // 如果没有data字段，尝试直接从根级别解析（兼容旧格式）
                fileName = jsonResponse.optString("name", null);
                fileSize = jsonResponse.optString("filesize", "0");
                downUrl = jsonResponse.optString("downUrl", null);
            }
            
            // 如果仍然没有找到下载链接，尝试搜索包含https://的字段
            if (downUrl == null || downUrl.trim().isEmpty()) {
                ApplicationLogger.logInfo("未找到下载链接字段，尝试搜索包含https://的链接");
                downUrl = extractUrlFromResponse(responseText);
            }
            
            if (downUrl != null && !downUrl.trim().isEmpty()) {
                ApplicationLogger.logInfo("找到下载链接: {}", downUrl.substring(0, Math.min(80, downUrl.length())) + "...");
            } else {
                ApplicationLogger.logError("未能从响应中提取到有效的下载链接");
                return new ApiResponse(false, null, null, 0, "未能获取到有效的下载链接");
            }
            
            // 解析文件大小
            long fileSizeBytes = parseFileSize(fileSize);
            
            return new ApiResponse(true, downUrl.trim(), fileName, fileSizeBytes, null);
            
        } catch (Exception e) {
            ApplicationLogger.logError("解析响应异常: {}", e.getMessage());
            return new ApiResponse(false, null, null, 0, "解析响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 从响应文本中提取包含https://的URL链接
     */
    private static String extractUrlFromResponse(String responseText) {
        if (responseText == null) {
            return null;
        }
        
        try {
            // 使用正则表达式查找https://开头的URL
            String regexPattern = "https://[^\"\\s]+";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regexPattern);
            java.util.regex.Matcher matcher = pattern.matcher(responseText);
            
            if (matcher.find()) {
                String foundUrl = matcher.group();
                ApplicationLogger.logInfo("通过正则匹配找到URL: {}", foundUrl);
                return foundUrl;
            }
            
            ApplicationLogger.logWarn("未能通过正则表达式找到https://链接");
            return null;
            
        } catch (Exception e) {
            ApplicationLogger.logError("提取URL时发生异常: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析文件大小字符串为字节数
     */
    private static long parseFileSize(String fileSizeStr) {
        if (fileSizeStr == null || fileSizeStr.trim().isEmpty()) {
            return 0;
        }
        
        try {
            return Long.parseLong(fileSizeStr.trim());
        } catch (NumberFormatException e) {
            // 解析带单位的格式
        }
        
        try {
            String trimmed = fileSizeStr.trim().toUpperCase();
            double size;
            long multiplier = 1;
            
            if (trimmed.endsWith("B")) {
                size = Double.parseDouble(trimmed.replaceAll("[^0-9.]", ""));
                multiplier = 1;
            } else if (trimmed.endsWith("K") || trimmed.endsWith("KB")) {
                size = Double.parseDouble(trimmed.replaceAll("[^0-9.]", ""));
                multiplier = 1024;
            } else if (trimmed.endsWith("M") || trimmed.endsWith("MB")) {
                size = Double.parseDouble(trimmed.replaceAll("[^0-9.]", ""));
                multiplier = 1024 * 1024;
            } else if (trimmed.endsWith("G") || trimmed.endsWith("GB")) {
                size = Double.parseDouble(trimmed.replaceAll("[^0-9.]", ""));
                multiplier = 1024 * 1024 * 1024;
            } else {
                size = Double.parseDouble(trimmed.replaceAll("[^0-9.]", ""));
            }
            
            return (long) (size * multiplier);
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 测试API连接
     */
    public static boolean testConnection() {
        try {
            URL url = new URL(API_BASE_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode == HttpURLConnection.HTTP_OK || 
                   responseCode == HttpURLConnection.HTTP_BAD_REQUEST;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 验证蓝奏云链接格式
     */
    public static boolean isValidLanzouUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String trimmedUrl = url.trim().toLowerCase();
        
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            return false;
        }
        
        return trimmedUrl.contains("lanzou") || trimmedUrl.contains("lanzo");
    }
    
    /**
     * 从URL提起文件名
     */
    public static String extractFileNameFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            URL urlObj = new URL(url);
            String path = urlObj.getPath();
            
            if (path != null && !path.isEmpty() && !path.equals("/")) {
                String[] segments = path.split("/");
                String lastSegment = segments[segments.length - 1];
                
                if (!lastSegment.isEmpty()) {
                    return lastSegment;
                }
            }
            
        } catch (Exception e) {
            // 忽略解析错误
        }
        
        return null;
    }
}