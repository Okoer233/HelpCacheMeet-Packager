package com.aeterhilrin.helpcachemeetpackager.file;

import com.aeterhilrin.helpcachemeetpackager.model.FileInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文件重命名器
 * 负责按照指定格式重命名下载的文件
 * 
 * @author AeterHilrin
 */
public class FileRenamer {
    
    // 文件名非法字符模式
    private static final Pattern INVALID_FILENAME_PATTERN = Pattern.compile(
        "[\\\\/:*?\"<>|]"
    );
    
    // 最大文件名长度
    private static final int MAX_FILENAME_LENGTH = 200;
    
    /**
     * 重命名结果类
     */
    public static class RenameResult {
        private final boolean success;
        private final String originalPath;
        private final String newPath;
        private final String errorMessage;
        
        public RenameResult(boolean success, String originalPath, String newPath, String errorMessage) {
            this.success = success;
            this.originalPath = originalPath;
            this.newPath = newPath;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public String getOriginalPath() { return originalPath; }
        public String getNewPath() { return newPath; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            return "RenameResult{" +
                    "success=" + success +
                    ", originalPath='" + originalPath + '\'' +
                    ", newPath='" + newPath + '\'' +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }
    
    /**
     * 批量重命名操作监听器
     */
    public interface BatchRenameListener {
        void onFileRenamed(String oldName, String newName, int currentFile, int totalFiles);
        void onRenameCompleted(List<RenameResult> results);
        void onRenameError(String errorMessage);
    }
    
    /**
     * 重命名单个文件
     * @param fileInfo 文件信息对象
     * @return 重命名结果
     */
    public static RenameResult renameFile(FileInfo fileInfo) {
        if (fileInfo == null) {
            return new RenameResult(false, null, null, "文件信息对象为空");
        }
        
        return renameFile(fileInfo.getFilePath(), fileInfo.getPrefix(), 
                         fileInfo.getOriginalFileName(), fileInfo.getSuffix());
    }
    
    /**
     * 重命名单个文件
     * @param originalPath 原始文件路径
     * @param prefix 前缀
     * @param originalFileName 原始文件名
     * @param suffix 后缀
     * @return 重命名结果
     */
    public static RenameResult renameFile(String originalPath, String prefix, 
                                        String originalFileName, int suffix) {
        
        if (originalPath == null || originalPath.trim().isEmpty()) {
            return new RenameResult(false, originalPath, null, "原始文件路径为空");
        }
        
        File originalFile = new File(originalPath);
        if (!originalFile.exists()) {
            return new RenameResult(false, originalPath, null, "原始文件不存在");
        }
        
        if (!originalFile.isFile()) {
            return new RenameResult(false, originalPath, null, "指定路径不是文件");
        }
        
        try {
            // 生成新文件名
            String newFileName = generateFormattedFileName(prefix, originalFileName, suffix);
            
            // 获取文件所在目录
            File parentDir = originalFile.getParentFile();
            String newPath = new File(parentDir, newFileName).getAbsolutePath();
            
            // 检查目标文件是否已存在
            File newFile = new File(newPath);
            if (newFile.exists() && !newFile.equals(originalFile)) {
                // 如果目标文件已存在且不是同一个文件，生成唯一文件名
                newPath = generateUniqueFileName(parentDir, newFileName);
            }
            
            // 执行重命名
            Path originalFilePath = Paths.get(originalPath);
            Path newFilePath = Paths.get(newPath);
            
            Files.move(originalFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            return new RenameResult(true, originalPath, newPath, null);
            
        } catch (Exception e) {
            return new RenameResult(false, originalPath, null, 
                "重命名失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量重命名文件
     * @param fileInfoList 文件信息列表
     * @param listener 批量重命名监听器
     * @return 重命名结果列表
     */
    public static List<RenameResult> batchRename(List<FileInfo> fileInfoList, 
                                               BatchRenameListener listener) {
        List<RenameResult> results = new ArrayList<>();
        
        if (fileInfoList == null || fileInfoList.isEmpty()) {
            if (listener != null) {
                listener.onRenameError("没有文件需要重命名");
            }
            return results;
        }
        
        try {
            for (int i = 0; i < fileInfoList.size(); i++) {
                FileInfo fileInfo = fileInfoList.get(i);
                
                RenameResult result = renameFile(fileInfo);
                results.add(result);
                
                if (result.isSuccess()) {
                    // 更新文件信息对象的路径
                    fileInfo.setFilePath(result.getNewPath());
                }
                
                if (listener != null) {
                    String oldName = new File(result.getOriginalPath()).getName();
                    String newName = result.isSuccess() ? 
                        new File(result.getNewPath()).getName() : oldName;
                    
                    listener.onFileRenamed(oldName, newName, i + 1, fileInfoList.size());
                }
            }
            
            if (listener != null) {
                listener.onRenameCompleted(results);
            }
            
        } catch (Exception e) {
            if (listener != null) {
                listener.onRenameError("批量重命名过程中发生错误: " + e.getMessage());
            }
        }
        
        return results;
    }
    
    /**
     * 生成格式化的文件名
     * 格式: [前缀]原文件名[后缀].zip
     */
    public static String generateFormattedFileName(String prefix, String originalFileName, int suffix) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            originalFileName = "unknown";
        }
        
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "File";
        }
        
        // 清理文件名中的非法字符
        String cleanOriginalName = sanitizeFileName(originalFileName.trim());
        String cleanPrefix = sanitizeFileName(prefix.trim());
        
        // 移除原文件名中的.zip扩展名（如果有）
        if (cleanOriginalName.toLowerCase().endsWith(".zip")) {
            cleanOriginalName = cleanOriginalName.substring(0, cleanOriginalName.length() - 4);
        }
        
        // 生成格式化文件名: [前缀]原文件名[后缀].zip
        String formattedName = String.format("[%s]%s[%d].zip", cleanPrefix, cleanOriginalName, suffix);
        
        // 检查文件名长度
        if (formattedName.length() > MAX_FILENAME_LENGTH) {
            formattedName = truncateFileName(formattedName, MAX_FILENAME_LENGTH);
        }
        
        return formattedName;
    }
    
    /**
     * 清理文件名中的非法字符
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        
        // 替换非法字符为下划线
        String sanitized = INVALID_FILENAME_PATTERN.matcher(fileName).replaceAll("_");
        
        // 移除前导和尾随的点和空格
        sanitized = sanitized.replaceAll("^[\\.\\s]+|[\\.\\s]+$", "");
        
        // 如果清理后为空，使用默认名称
        if (sanitized.isEmpty()) {
            sanitized = "file";
        }
        
        return sanitized;
    }
    
    /**
     * 截断过长的文件名
     */
    private static String truncateFileName(String fileName, int maxLength) {
        if (fileName.length() <= maxLength) {
            return fileName;
        }
        
        // 尝试保留文件扩展名
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            String extension = fileName.substring(lastDotIndex);
            String nameWithoutExt = fileName.substring(0, lastDotIndex);
            
            int maxNameLength = maxLength - extension.length();
            if (maxNameLength > 0) {
                return nameWithoutExt.substring(0, Math.min(nameWithoutExt.length(), maxNameLength)) + extension;
            }
        }
        
        // 如果无法保留扩展名，直接截断
        return fileName.substring(0, maxLength);
    }
    
    /**
     * 生成唯一文件名
     */
    private static String generateUniqueFileName(File parentDir, String baseFileName) {
        String nameWithoutExt;
        String extension;
        
        int lastDotIndex = baseFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            nameWithoutExt = baseFileName.substring(0, lastDotIndex);
            extension = baseFileName.substring(lastDotIndex);
        } else {
            nameWithoutExt = baseFileName;
            extension = "";
        }
        
        int counter = 1;
        String uniqueFileName;
        
        do {
            uniqueFileName = nameWithoutExt + "_" + counter + extension;
            counter++;
        } while (new File(parentDir, uniqueFileName).exists() && counter < 1000);
        
        return new File(parentDir, uniqueFileName).getAbsolutePath();
    }
    
    /**
     * 验证重命名操作的可行性
     * @param fileInfo 文件信息
     * @return 验证结果消息，null表示验证通过
     */
    public static String validateRename(FileInfo fileInfo) {
        if (fileInfo == null) {
            return "文件信息对象为空";
        }
        
        if (fileInfo.getFilePath() == null || fileInfo.getFilePath().trim().isEmpty()) {
            return "文件路径为空";
        }
        
        File file = new File(fileInfo.getFilePath());
        if (!file.exists()) {
            return "文件不存在: " + fileInfo.getFilePath();
        }
        
        if (!file.isFile()) {
            return "指定路径不是文件: " + fileInfo.getFilePath();
        }
        
        if (!file.canRead()) {
            return "文件无法读取: " + fileInfo.getFilePath();
        }
        
        // 检查父目录写权限
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.canWrite()) {
            return "目录无写权限: " + parentDir.getAbsolutePath();
        }
        
        // 验证新文件名的合法性
        try {
            String newFileName = generateFormattedFileName(
                fileInfo.getPrefix(), 
                fileInfo.getOriginalFileName(), 
                fileInfo.getSuffix()
            );
            
            if (newFileName.length() > MAX_FILENAME_LENGTH) {
                return "生成的文件名过长";
            }
            
        } catch (Exception e) {
            return "生成新文件名时出错: " + e.getMessage();
        }
        
        return null; // 验证通过
    }
    
    /**
     * 预览重命名结果
     * @param fileInfoList 文件信息列表
     * @return 重命名预览列表（原文件名 -> 新文件名）
     */
    public static List<String> previewRename(List<FileInfo> fileInfoList) {
        List<String> previews = new ArrayList<>();
        
        if (fileInfoList == null || fileInfoList.isEmpty()) {
            return previews;
        }
        
        for (FileInfo fileInfo : fileInfoList) {
            try {
                String originalName = new File(fileInfo.getFilePath()).getName();
                String newName = generateFormattedFileName(
                    fileInfo.getPrefix(),
                    fileInfo.getOriginalFileName(),
                    fileInfo.getSuffix()
                );
                
                previews.add(originalName + " -> " + newName);
                
            } catch (Exception e) {
                previews.add(fileInfo.getOriginalFileName() + " -> [错误: " + e.getMessage() + "]");
            }
        }
        
        return previews;
    }
    
    /**
     * 检查文件名是否已经是格式化的
     * @param fileName 文件名
     * @return 是否已格式化
     */
    public static boolean isFormattedFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否符合 [前缀]文件名[后缀] 的格式
        Pattern formattedPattern = Pattern.compile("^\\[.+\\].+\\[\\d+\\].*$");
        return formattedPattern.matcher(fileName).matches();
    }
    
    /**
     * 从格式化文件名中提取信息
     * @param formattedFileName 格式化的文件名
     * @return 提取的信息数组 [前缀, 原文件名, 后缀]，失败返回null
     */
    public static String[] extractInfoFromFormattedName(String formattedFileName) {
        if (!isFormattedFileName(formattedFileName)) {
            return null;
        }
        
        try {
            // 使用正则表达式提取信息
            Pattern pattern = Pattern.compile("^\\[(.+?)\\](.+?)\\[(\\d+)\\](.*)$");
            java.util.regex.Matcher matcher = pattern.matcher(formattedFileName);
            
            if (matcher.matches()) {
                String prefix = matcher.group(1);
                String fileName = matcher.group(2) + matcher.group(4); // 文件名 + 扩展名
                String suffixStr = matcher.group(3);
                
                return new String[]{prefix, fileName, suffixStr};
            }
            
        } catch (Exception e) {
            // 解析失败
        }
        
        return null;
    }
}