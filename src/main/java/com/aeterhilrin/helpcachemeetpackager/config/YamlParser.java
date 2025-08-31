package com.aeterhilrin.helpcachemeetpackager.config;

import com.aeterhilrin.helpcachemeetpackager.model.ProjectConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YAML配置文件解析器
 * 负责解析用户配置文件并转换为ProjectConfig对象
 * 
 * @author AeterHilrin
 */
public class YamlParser {
    
    private static final Pattern ITEM_PATTERN = Pattern.compile(
        "^(\\S+)\\s+\"([^\"]+)\"\\s+(\\S+)\\s+(\\d+)$"
    );
    
    /**
     * 从文件路径解析配置
     * @param filePath 配置文件路径
     * @return 解析后的项目配置
     * @throws Exception 解析异常
     */
    public static ProjectConfig parseFromFile(String filePath) throws Exception {
        return parseFromFile(new File(filePath));
    }
    
    /**
     * 从文件对象解析配置
     * @param file 配置文件
     * @return 解析后的项目配置
     * @throws Exception 解析异常
     */
    public static ProjectConfig parseFromFile(File file) throws Exception {
        if (!file.exists()) {
            throw new FileNotFoundException("配置文件不存在: " + file.getPath());
        }
        
        if (!file.canRead()) {
            throw new IOException("无法读取配置文件: " + file.getPath());
        }
        
        // 读取文件内容
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return parseFromString(content);
    }
    
    /**
     * 从字符串内容解析配置
     * @param content YAML内容
     * @return 解析后的项目配置
     * @throws Exception 解析异常
     */
    public static ProjectConfig parseFromString(String content) throws Exception {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("配置文件内容为空");
        }
        
        try {
            // 首先尝试标准YAML解析
            return parseStandardYaml(content);
        } catch (Exception e) {
            // 如果标准YAML解析失败，尝试自定义格式解析
            return parseCustomFormat(content);
        }
    }
    
    /**
     * 解析标准YAML格式
     * 格式示例:
     * 项目名称: TestProject
     * 下载项目:
     *   - 前缀: Tech
     *     链接: "http://test.lanzou.com/test1"
     *     密码: 123
     *     后缀: 0
     */
    private static ProjectConfig parseStandardYaml(String content) throws Exception {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(content);
        
        ProjectConfig config = new ProjectConfig();
        
        // 解析项目名称
        String projectName = extractProjectName(data);
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("配置文件中缺少项目名称");
        }
        config.setProjectName(projectName);
        
        // 解析下载项目列表
        Object itemsObj = data.get("下载项目");
        if (itemsObj == null) {
            itemsObj = data.get("items");
        }
        
        if (itemsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsList = (List<Map<String, Object>>) itemsObj;
            
            for (Map<String, Object> itemMap : itemsList) {
                ProjectConfig.DownloadItem item = parseDownloadItem(itemMap);
                config.addItem(item);
            }
        }
        
        if (config.getItems().isEmpty()) {
            throw new IllegalArgumentException("配置文件中没有有效的下载项目");
        }
        
        return config;
    }
    
    /**
     * 解析自定义格式
     * 格式示例:
     * 项目名称: TestProject
     * TestPrefix "http://test.lanzou.com/test1" 123 0
     * AnotherPrefix "http://test.lanzou.com/test2" 无 1
     */
    private static ProjectConfig parseCustomFormat(String content) throws Exception {
        ProjectConfig config = new ProjectConfig();
        
        String[] lines = content.split("\\r?\\n");
        String projectName = null;
        
        for (String line : lines) {
            line = line.trim();
            
            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // 解析项目名称
            if (projectName == null && (line.startsWith("项目名称:") || line.startsWith("项目名称："))) {
                projectName = line.substring(line.indexOf(":") + 1).trim();
                if (projectName.isEmpty()) {
                    projectName = line.substring(line.indexOf("：") + 1).trim();
                }
                config.setProjectName(projectName);
                continue;
            }
            
            // 解析下载项目行
            if (projectName != null) {
                ProjectConfig.DownloadItem item = parseItemLine(line);
                if (item != null) {
                    config.addItem(item);
                }
            }
        }
        
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("配置文件中缺少项目名称");
        }
        
        if (config.getItems().isEmpty()) {
            throw new IllegalArgumentException("配置文件中没有有效的下载项目");
        }
        
        return config;
    }
    
    /**
     * 从Map中解析下载项目
     */
    private static ProjectConfig.DownloadItem parseDownloadItem(Map<String, Object> itemMap) throws Exception {
        String prefix = getString(itemMap, "前缀", "prefix");
        String url = getString(itemMap, "链接", "url", "lanzouUrl");
        String password = getString(itemMap, "密码", "password", "pwd");
        Integer suffix = getInteger(itemMap, "后缀", "suffix");
        
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("下载项目缺少前缀");
        }
        
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("下载项目缺少链接");
        }
        
        if (suffix == null) {
            throw new IllegalArgumentException("下载项目缺少后缀");
        }
        
        // 处理密码
        if (password == null || "无".equals(password) || password.trim().isEmpty()) {
            password = null;
        }
        
        return new ProjectConfig.DownloadItem(prefix, url, password, suffix);
    }
    
    /**
     * 解析单行下载项目
     * 格式: 前缀 "链接" 密码 后缀
     */
    private static ProjectConfig.DownloadItem parseItemLine(String line) {
        Matcher matcher = ITEM_PATTERN.matcher(line);
        
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            String url = matcher.group(2);
            String password = matcher.group(3);
            int suffix = Integer.parseInt(matcher.group(4));
            
            // 处理密码
            if ("无".equals(password) || password.trim().isEmpty()) {
                password = null;
            }
            
            return new ProjectConfig.DownloadItem(prefix, url, password, suffix);
        }
        
        return null;
    }
    
    /**
     * 提取项目名称
     */
    private static String extractProjectName(Map<String, Object> data) {
        // 尝试多种可能的键名
        String[] possibleKeys = {"项目名称", "projectName", "name", "项目", "project"};
        
        for (String key : possibleKeys) {
            Object value = data.get(key);
            if (value != null) {
                return value.toString().trim();
            }
        }
        
        return null;
    }
    
    /**
     * 从Map中获取字符串值
     */
    private static String getString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }
    
    /**
     * 从Map中获取整数值
     */
    private static Integer getInteger(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                if (value instanceof Integer) {
                    return (Integer) value;
                } else {
                    try {
                        return Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) {
                        // 继续尝试其他键
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 验证配置文件格式
     * @param file 配置文件
     * @return 验证结果信息
     */
    public static ConfigValidationResult validateConfig(File file) {
        try {
            ProjectConfig config = parseFromFile(file);
            
            if (!config.isValid()) {
                return new ConfigValidationResult(false, "配置文件格式无效");
            }
            
            // 验证每个下载项目
            for (int i = 0; i < config.getItems().size(); i++) {
                ProjectConfig.DownloadItem item = config.getItems().get(i);
                if (!item.isValid()) {
                    return new ConfigValidationResult(false, 
                        String.format("第 %d 个下载项目配置无效", i + 1));
                }
            }
            
            return new ConfigValidationResult(true, "配置文件格式正确");
            
        } catch (Exception e) {
            return new ConfigValidationResult(false, "配置文件解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 配置验证结果
     */
    public static class ConfigValidationResult {
        private final boolean valid;
        private final String message;
        
        public ConfigValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * 生成示例配置文件内容
     * @return 示例配置文件内容
     */
    public static String generateSampleConfig() {
        return "# HelpCacheMeetPackager 配置文件示例\n" +
               "# \n" +
               "# 格式说明:\n" +
               "# 项目名称: 你的项目名称\n" +
               "# 前缀 \"蓝奏云链接\" 密码 后缀数字\n" +
               "#\n" +
               "# 注意:\n" +
               "# - 如果没有密码，请填写 无\n" +
               "# - 后缀数字用于文件排序，从0开始\n" +
               "# - 蓝奏云链接需要用双引号包围\n" +
               "\n" +
               "项目名称: 示例项目\n" +
               "\n" +
               "Tech \"https://lanzou.com/ixxxxxx\" 123456 0\n" +
               "Do \"https://lanzou.com/ixxxxxx\" 无 1\n" +
               "Do2 \"https://lanzou.com/ixxxxxx\" 654321 2\n";
    }
}