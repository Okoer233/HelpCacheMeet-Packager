package com.aeterhilrin.helpcachemeetpackager.test;

import com.aeterhilrin.helpcachemeetpackager.config.YamlParser;
import com.aeterhilrin.helpcachemeetpackager.config.ConfigValidator;
import com.aeterhilrin.helpcachemeetpackager.model.ProjectConfig;

import java.io.File;

/**
 * YAML配置解析测试类
 * 
 * @author AeterHilrin
 */
public class YamlParseTest {
    
    public static void main(String[] args) {
        testConfigParsing();
    }
    
    public static void testConfigParsing() {
        String testConfigPath = "user-test-config.yaml";
        File configFile = new File(testConfigPath);
        
        System.out.println("=== YAML配置解析测试 ===");
        System.out.println("测试文件: " + configFile.getAbsolutePath());
        System.out.println("文件存在: " + configFile.exists());
        
        if (!configFile.exists()) {
            System.out.println("错误: 测试配置文件不存在");
            return;
        }
        
        try {
            // 步骤1: 验证配置文件
            System.out.println("\n--- 步骤1: 验证配置文件 ---");
            ConfigValidator.ValidationResult validationResult = ConfigValidator.validateConfigFile(configFile);
            System.out.println("验证结果: " + (validationResult.isValid() ? "通过" : "失败"));
            
            if (validationResult.hasErrors()) {
                System.out.println("错误信息:");
                for (String error : validationResult.getErrors()) {
                    System.out.println("  • " + error);
                }
            }
            
            if (validationResult.hasWarnings()) {
                System.out.println("警告信息:");
                for (String warning : validationResult.getWarnings()) {
                    System.out.println("  • " + warning);
                }
            }
            
            if (!validationResult.isValid()) {
                System.out.println("配置文件验证失败，无法继续");
                return;
            }
            
            // 步骤2: 解析配置文件
            System.out.println("\n--- 步骤2: 解析配置文件 ---");
            ProjectConfig config = YamlParser.parseFromFile(configFile);
            
            System.out.println("项目名称: " + config.getProjectName());
            System.out.println("下载项目数量: " + config.getItems().size());
            
            for (int i = 0; i < config.getItems().size(); i++) {
                ProjectConfig.DownloadItem item = config.getItems().get(i);
                System.out.println("项目 " + (i + 1) + ":");
                System.out.println("  前缀: " + item.getPrefix());
                System.out.println("  链接: " + item.getLanzouUrl());
                System.out.println("  密码: " + (item.getPassword() != null ? item.getPassword() : "无"));
                System.out.println("  后缀: " + item.getSuffix());
            }
            
            // 步骤3: 验证配置有效性
            System.out.println("\n--- 步骤3: 验证配置有效性 ---");
            System.out.println("配置有效: " + config.isValid());
            
            System.out.println("\n=== 测试完成 ===");
            
        } catch (Exception e) {
            System.out.println("测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}