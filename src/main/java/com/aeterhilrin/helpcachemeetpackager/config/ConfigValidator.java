package com.aeterhilrin.helpcachemeetpackager.config;

import com.aeterhilrin.helpcachemeetpackager.model.ProjectConfig;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 配置文件验证器
 * 验证配置文件的完整性和正确性
 * 
 * @author AeterHilrin
 */
public class ConfigValidator {
    
    // 蓝奏云域名模式
    private static final Pattern LANZOU_DOMAIN_PATTERN = Pattern.compile(
        ".*(?:lanzou[a-z]*|lanzo[a-z]*)\\.com.*",
        Pattern.CASE_INSENSITIVE
    );
    
    // 项目名称非法字符模式
    private static final Pattern INVALID_PROJECT_NAME_PATTERN = Pattern.compile(
        "[\\\\/:*?\"<>|]"
    );
    
    /**
     * 验证配置文件
     * @param file 配置文件
     * @return 验证结果
     */
    public static ValidationResult validateConfigFile(File file) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 检查文件基本属性
        if (!validateFileBasic(file, errors)) {
            return new ValidationResult(false, errors, warnings);
        }
        
        try {
            // 解析配置文件
            ProjectConfig config = YamlParser.parseFromFile(file);
            return validateProjectConfig(config);
            
        } catch (Exception e) {
            errors.add("配置文件解析失败: " + e.getMessage());
            return new ValidationResult(false, errors, warnings);
        }
    }
    
    /**
     * 验证项目配置对象
     * @param config 项目配置
     * @return 验证结果
     */
    public static ValidationResult validateProjectConfig(ProjectConfig config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (config == null) {
            errors.add("配置对象为空");
            return new ValidationResult(false, errors, warnings);
        }
        
        // 验证项目名称
        validateProjectName(config.getProjectName(), errors, warnings);
        
        // 验证下载项目列表
        validateDownloadItems(config.getItems(), errors, warnings);
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    /**
     * 验证文件基本属性
     */
    private static boolean validateFileBasic(File file, List<String> errors) {
        if (file == null) {
            errors.add("文件对象为空");
            return false;
        }
        
        if (!file.exists()) {
            errors.add("文件不存在: " + file.getPath());
            return false;
        }
        
        if (!file.isFile()) {
            errors.add("指定路径不是文件: " + file.getPath());
            return false;
        }
        
        if (!file.canRead()) {
            errors.add("文件无法读取: " + file.getPath());
            return false;
        }
        
        // 检查文件扩展名
        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".yaml") && !fileName.endsWith(".yml")) {
            errors.add("文件类型不正确，请使用 .yaml 或 .yml 文件");
            return false;
        }
        
        // 检查文件大小
        long fileSize = file.length();
        if (fileSize == 0) {
            errors.add("配置文件为空");
            return false;
        }
        
        if (fileSize > 1024 * 1024) { // 1MB
            errors.add("配置文件过大，请检查文件内容");
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证项目名称
     */
    private static void validateProjectName(String projectName, List<String> errors, List<String> warnings) {
        if (projectName == null || projectName.trim().isEmpty()) {
            errors.add("项目名称不能为空");
            return;
        }
        
        String trimmedName = projectName.trim();
        
        // 检查长度
        if (trimmedName.length() > 50) {
            warnings.add("项目名称过长，建议控制在50个字符以内");
        }
        
        // 检查非法字符
        if (INVALID_PROJECT_NAME_PATTERN.matcher(trimmedName).find()) {
            errors.add("项目名称包含非法字符，不能包含: \\ / : * ? \" < > |");
        }
        
        // 检查特殊情况
        if (trimmedName.equals(".") || trimmedName.equals("..")) {
            errors.add("项目名称不能为 '.' 或 '..'");
        }
        
        // 检查是否以空格开头或结尾
        if (!projectName.equals(trimmedName)) {
            warnings.add("项目名称包含前导或尾随空格，将被自动去除");
        }
    }
    
    /**
     * 验证下载项目列表
     */
    private static void validateDownloadItems(List<ProjectConfig.DownloadItem> items, 
                                            List<String> errors, List<String> warnings) {
        if (items == null || items.isEmpty()) {
            errors.add("没有配置任何下载项目");
            return;
        }
        
        Set<String> prefixes = new HashSet<>();
        Set<Integer> suffixes = new HashSet<>();
        Set<String> urls = new HashSet<>();
        
        for (int i = 0; i < items.size(); i++) {
            ProjectConfig.DownloadItem item = items.get(i);
            String itemDesc = String.format("第 %d 个下载项目", i + 1);
            
            validateDownloadItem(item, itemDesc, errors, warnings);
            
            // 检查重复的前缀
            if (item.getPrefix() != null) {
                if (prefixes.contains(item.getPrefix())) {
                    warnings.add(itemDesc + " 的前缀 '" + item.getPrefix() + "' 与其他项目重复");
                } else {
                    prefixes.add(item.getPrefix());
                }
            }
            
            // 检查重复的后缀
            if (suffixes.contains(item.getSuffix())) {
                warnings.add(itemDesc + " 的后缀 " + item.getSuffix() + " 与其他项目重复");
            } else {
                suffixes.add(item.getSuffix());
            }
            
            // 检查重复的URL
            if (item.getLanzouUrl() != null) {
                if (urls.contains(item.getLanzouUrl())) {
                    warnings.add(itemDesc + " 的链接与其他项目重复");
                } else {
                    urls.add(item.getLanzouUrl());
                }
            }
        }
        
        // 检查后缀连续性
        validateSuffixContinuity(suffixes, warnings);
    }
    
    /**
     * 验证单个下载项目
     */
    private static void validateDownloadItem(ProjectConfig.DownloadItem item, String itemDesc,
                                           List<String> errors, List<String> warnings) {
        if (item == null) {
            errors.add(itemDesc + " 配置为空");
            return;
        }
        
        // 验证前缀
        validatePrefix(item.getPrefix(), itemDesc, errors, warnings);
        
        // 验证链接
        validateLanzouUrl(item.getLanzouUrl(), itemDesc, errors, warnings);
        
        // 验证密码
        validatePassword(item.getPassword(), itemDesc, warnings);
        
        // 验证后缀
        validateSuffix(item.getSuffix(), itemDesc, errors, warnings);
    }
    
    /**
     * 验证前缀
     */
    private static void validatePrefix(String prefix, String itemDesc, 
                                     List<String> errors, List<String> warnings) {
        if (prefix == null || prefix.trim().isEmpty()) {
            errors.add(itemDesc + " 缺少前缀");
            return;
        }
        
        String trimmedPrefix = prefix.trim();
        
        // 检查长度
        if (trimmedPrefix.length() > 20) {
            warnings.add(itemDesc + " 前缀过长，建议控制在20个字符以内");
        }
        
        // 检查特殊字符
        if (trimmedPrefix.contains("[") || trimmedPrefix.contains("]")) {
            warnings.add(itemDesc + " 前缀包含方括号，可能影响文件名格式");
        }
        
        // 检查空格
        if (trimmedPrefix.contains(" ")) {
            warnings.add(itemDesc + " 前缀包含空格，可能影响文件处理");
        }
    }
    
    /**
     * 验证蓝奏云链接
     */
    private static void validateLanzouUrl(String url, String itemDesc,
                                        List<String> errors, List<String> warnings) {
        if (url == null || url.trim().isEmpty()) {
            errors.add(itemDesc + " 缺少蓝奏云链接");
            return;
        }
        
        String trimmedUrl = url.trim();
        
        // 检查URL格式
        try {
            new URL(trimmedUrl);
        } catch (MalformedURLException e) {
            errors.add(itemDesc + " 链接格式不正确: " + trimmedUrl);
            return;
        }
        
        // 检查是否为蓝奏云域名
        if (!LANZOU_DOMAIN_PATTERN.matcher(trimmedUrl).matches()) {
            warnings.add(itemDesc + " 链接似乎不是蓝奏云域名，请确认链接正确");
        }
        
        // 检查协议
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            errors.add(itemDesc + " 链接必须以 http:// 或 https:// 开头");
        }
        
        // 检查链接长度
        if (trimmedUrl.length() > 200) {
            warnings.add(itemDesc + " 链接过长，请检查是否正确");
        }
    }
    
    /**
     * 验证密码
     */
    private static void validatePassword(String password, String itemDesc, List<String> warnings) {
        if (password != null && !password.trim().isEmpty() && !"无".equals(password.trim())) {
            String trimmedPassword = password.trim();
            
            // 检查密码长度
            if (trimmedPassword.length() > 20) {
                warnings.add(itemDesc + " 密码过长，请确认是否正确");
            }
            
            // 检查密码复杂度（简单检查）
            if (trimmedPassword.length() < 3) {
                warnings.add(itemDesc + " 密码过短，请确认是否正确");
            }
        }
    }
    
    /**
     * 验证后缀
     */
    private static void validateSuffix(int suffix, String itemDesc,
                                     List<String> errors, List<String> warnings) {
        if (suffix < 0) {
            errors.add(itemDesc + " 后缀不能为负数");
        }
        
        if (suffix > 999) {
            warnings.add(itemDesc + " 后缀数字过大，建议控制在合理范围内");
        }
    }
    
    /**
     * 验证后缀连续性
     */
    private static void validateSuffixContinuity(Set<Integer> suffixes, List<String> warnings) {
        if (suffixes.size() <= 1) {
            return;
        }
        
        List<Integer> sortedSuffixes = new ArrayList<>(suffixes);
        sortedSuffixes.sort(Integer::compareTo);
        
        // 检查是否从0开始
        if (sortedSuffixes.get(0) != 0) {
            warnings.add("后缀编号建议从0开始，当前最小值为: " + sortedSuffixes.get(0));
        }
        
        // 检查是否连续
        for (int i = 1; i < sortedSuffixes.size(); i++) {
            int prev = sortedSuffixes.get(i - 1);
            int current = sortedSuffixes.get(i);
            
            if (current - prev > 1) {
                warnings.add("后缀编号不连续，在 " + prev + " 和 " + current + " 之间有间隔");
                break; // 只提示第一个间隔
            }
        }
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public String getFormattedMessage() {
            StringBuilder sb = new StringBuilder();
            
            if (hasErrors()) {
                sb.append("错误:\n");
                for (String error : errors) {
                    sb.append("  • ").append(error).append("\n");
                }
            }
            
            if (hasWarnings()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append("警告:\n");
                for (String warning : warnings) {
                    sb.append("  • ").append(warning).append("\n");
                }
            }
            
            if (!hasErrors() && !hasWarnings()) {
                sb.append("配置文件格式正确，没有发现问题。");
            }
            
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", errors=" + errors.size() +
                    ", warnings=" + warnings.size() +
                    '}';
        }
    }
}