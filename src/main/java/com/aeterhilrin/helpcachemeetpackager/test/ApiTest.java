package com.aeterhilrin.helpcachemeetpackager.test;

import com.aeterhilrin.helpcachemeetpackager.download.LanzouApiClient;

/**
 * API接口测试类
 * 用于测试新的蓝奏云解析API接口
 * 
 * @author AeterHilrin
 */
public class ApiTest {
    
    public static void main(String[] args) {
        System.out.println("=== 蓝奏云API接口测试 ===");
        System.out.println();
        
        // 测试API连接
        testConnection();
        
        // 测试链接格式验证
        testUrlValidation();
        
        // 测试API解析（需要真实链接）
        // testApiParsing();
        
        System.out.println("测试完成！");
    }
    
    /**
     * 测试API连接
     */
    private static void testConnection() {
        System.out.println("1. 测试API连接...");
        
        boolean connected = LanzouApiClient.testConnection();
        System.out.println("API连接状态: " + (connected ? "成功" : "失败"));
        System.out.println();
    }
    
    /**
     * 测试链接格式验证
     */
    private static void testUrlValidation() {
        System.out.println("2. 测试链接格式验证...");
        
        String[] testUrls = {
            "https://lanzou.com/iEuEY25ht7ri",
            "https://ilingku.lanzoub.com/iEuEY25ht7ri", 
            "https://wwi.lanzoui.com/iEuEY25ht7ri",
            "http://invalid-url.com/test",
            "not-a-url",
            null,
            ""
        };
        
        for (String url : testUrls) {
            boolean valid = LanzouApiClient.isValidLanzouUrl(url);
            System.out.println("URL: " + (url != null ? url : "null") + " -> " + (valid ? "有效" : "无效"));
        }
        System.out.println();
    }
    
    /**
     * 测试API解析功能
     * 注意：需要提供真实的蓝奏云链接进行测试
     */
    private static void testApiParsing() {
        System.out.println("3. 测试API解析...");
        
        // 示例链接（请替换为真实链接进行测试）
        String testUrl = "https://ilingku.lanzoub.com/iEuEY25ht7ri";
        String testPassword = ""; // 如果有密码请填写
        
        System.out.println("测试链接: " + testUrl);
        
        LanzouApiClient.ApiResponse response = LanzouApiClient.parseDirectUrl(testUrl, testPassword);
        
        System.out.println("解析结果:");
        System.out.println("  成功: " + response.isSuccess());
        
        if (response.isSuccess()) {
            System.out.println("  直链: " + response.getDirectUrl());
            System.out.println("  文件名: " + response.getFileName());
            System.out.println("  文件大小: " + response.getFileSize() + " 字节");
        } else {
            System.out.println("  错误信息: " + response.getErrorMessage());
        }
        
        System.out.println();
    }
}