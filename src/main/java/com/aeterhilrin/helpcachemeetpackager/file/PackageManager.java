package com.aeterhilrin.helpcachemeetpackager.file;

import com.aeterhilrin.helpcachemeetpackager.model.FileInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * 打包管理器
 * 负责管理文件的打包操作，协调解压和输出过程
 * 
 * @author AeterHilrin
 */
public class PackageManager {
    
    /**
     * 打包进度监听器接口
     */
    public interface PackageProgressListener {
        void onPackageStarted(String projectName, int totalFiles);
        void onFileProcessing(String fileName, int currentFile, int totalFiles);
        void onFileProcessed(String fileName, int currentFile, int totalFiles);
        void onPackageCompleted(String outputPath, PackageResult result);
        void onPackageError(String errorMessage);
        void onConflictResolved(String fileName, String action);
    }
    
    /**
     * 打包结果类
     */
    public static class PackageResult {
        private final boolean success;
        private final String outputPath;
        private final List<String> processedFiles;
        private final List<String> conflictFiles;
        private final List<String> errors;
        private final long startTime;
        private final long endTime;
        
        public PackageResult(boolean success, String outputPath, 
                           List<String> processedFiles, List<String> conflictFiles,
                           List<String> errors, long startTime, long endTime) {
            this.success = success;
            this.outputPath = outputPath;
            this.processedFiles = processedFiles != null ? new ArrayList<>(processedFiles) : new ArrayList<>();
            this.conflictFiles = conflictFiles != null ? new ArrayList<>(conflictFiles) : new ArrayList<>();
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        public boolean isSuccess() { return success; }
        public String getOutputPath() { return outputPath; }
        public List<String> getProcessedFiles() { return new ArrayList<>(processedFiles); }
        public List<String> getConflictFiles() { return new ArrayList<>(conflictFiles); }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public long getDuration() { return endTime - startTime; }
        
        public String getFormattedDuration() {
            long durationMs = getDuration();
            if (durationMs < 1000) {
                return durationMs + " ms";
            } else if (durationMs < 60000) {
                return String.format("%.1f s", durationMs / 1000.0);
            } else {
                long minutes = durationMs / 60000;
                long seconds = (durationMs % 60000) / 1000;
                return String.format("%d:%02d", minutes, seconds);
            }
        }
        
        @Override
        public String toString() {
            return "PackageResult{" +
                    "success=" + success +
                    ", outputPath='" + outputPath + '\'' +
                    ", processedFiles=" + processedFiles.size() +
                    ", conflictFiles=" + conflictFiles.size() +
                    ", errors=" + errors.size() +
                    ", duration=" + getFormattedDuration() +
                    '}';
        }
    }
    
    /**
     * 同步打包文件
     * @param selectedFiles 选中的文件列表
     * @param projectName 项目名称
     * @param progressListener 进度监听器
     * @return 打包结果
     */
    public static PackageResult packageFiles(List<FileInfo> selectedFiles, String projectName,
                                           PackageProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        
        try {
            // 验证输入参数
            String validationError = validatePackageInput(selectedFiles, projectName);
            if (validationError != null) {
                errors.add(validationError);
                return new PackageResult(false, null, null, null, errors, startTime, System.currentTimeMillis());
            }
            
            if (progressListener != null) {
                progressListener.onPackageStarted(projectName, selectedFiles.size());
            }
            
            // 按后缀排序文件
            List<FileInfo> sortedFiles = new ArrayList<>(selectedFiles);
            sortedFiles.sort(Comparator.comparing(FileInfo::getSuffix));
            
            // 创建解压进度监听器
            ZipExtractor.ExtractionProgressListener extractionListener = 
                new ZipExtractor.ExtractionProgressListener() {
                    @Override
                    public void onFileExtracted(String fileName, int currentFile, int totalFiles) {
                        if (progressListener != null) {
                            progressListener.onFileProcessed(fileName, currentFile, totalFiles);
                        }
                    }
                    
                    @Override
                    public void onExtractionCompleted(String outputPath) {
                        // 在外部处理
                    }
                    
                    @Override
                    public void onExtractionError(String errorMessage) {
                        errors.add(errorMessage);
                    }
                    
                    @Override
                    public void onFileConflict(String fileName, String action) {
                        if (progressListener != null) {
                            progressListener.onConflictResolved(fileName, action);
                        }
                    }
                };
            
            // 执行解压操作
            ZipExtractor.ExtractionResult extractionResult = ZipExtractor.extractFiles(
                sortedFiles, projectName, extractionListener
            );
            
            long endTime = System.currentTimeMillis();
            
            if (extractionResult.isSuccess()) {
                PackageResult result = new PackageResult(
                    true,
                    extractionResult.getOutputPath(),
                    extractionResult.getExtractedFiles(),
                    extractionResult.getConflictFiles(),
                    errors,
                    startTime,
                    endTime
                );
                
                if (progressListener != null) {
                    progressListener.onPackageCompleted(extractionResult.getOutputPath(), result);
                }
                
                return result;
            } else {
                errors.add(extractionResult.getErrorMessage());
                
                PackageResult result = new PackageResult(
                    false,
                    null,
                    null,
                    null,
                    errors,
                    startTime,
                    endTime
                );
                
                if (progressListener != null) {
                    progressListener.onPackageError("打包失败: " + extractionResult.getErrorMessage());
                }
                
                return result;
            }
            
        } catch (Exception e) {
            errors.add("打包过程中发生异常: " + e.getMessage());
            
            if (progressListener != null) {
                progressListener.onPackageError(e.getMessage());
            }
            
            return new PackageResult(false, null, null, null, errors, 
                                   startTime, System.currentTimeMillis());
        }
    }
    
    /**
     * 异步打包文件
     * @param selectedFiles 选中的文件列表
     * @param projectName 项目名称
     * @param progressListener 进度监听器
     * @return CompletableFuture包装的打包结果
     */
    public static CompletableFuture<PackageResult> packageFilesAsync(
            List<FileInfo> selectedFiles, String projectName,
            PackageProgressListener progressListener) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return packageFiles(selectedFiles, projectName, progressListener);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * 验证打包输入参数
     */
    private static String validatePackageInput(List<FileInfo> selectedFiles, String projectName) {
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return "没有选择要打包的文件";
        }
        
        if (projectName == null || projectName.trim().isEmpty()) {
            return "项目名称不能为空";
        }
        
        // 验证每个文件
        for (int i = 0; i < selectedFiles.size(); i++) {
            FileInfo fileInfo = selectedFiles.get(i);
            String fileValidationError = validateFileInfo(fileInfo, i + 1);
            if (fileValidationError != null) {
                return fileValidationError;
            }
        }
        
        return null; // 验证通过
    }
    
    /**
     * 验证单个文件信息
     */
    private static String validateFileInfo(FileInfo fileInfo, int index) {
        if (fileInfo == null) {
            return String.format("第 %d 个文件信息为空", index);
        }
        
        if (fileInfo.getFilePath() == null || fileInfo.getFilePath().trim().isEmpty()) {
            return String.format("第 %d 个文件路径为空", index);
        }
        
        File file = new File(fileInfo.getFilePath());
        if (!file.exists()) {
            return String.format("第 %d 个文件不存在: %s", index, fileInfo.getFilePath());
        }
        
        if (!file.isFile()) {
            return String.format("第 %d 个路径不是文件: %s", index, fileInfo.getFilePath());
        }
        
        if (!file.canRead()) {
            return String.format("第 %d 个文件无法读取: %s", index, fileInfo.getFilePath());
        }
        
        return null; // 验证通过
    }
    
    /**
     * 清理输出目录
     * @param outputPath 输出目录路径
     * @return 清理是否成功
     */
    public static boolean cleanupOutputDirectory(String outputPath) {
        if (outputPath == null || outputPath.trim().isEmpty()) {
            return false;
        }
        
        try {
            Path path = Paths.get(outputPath);
            if (Files.exists(path) && Files.isDirectory(path)) {
                // 递归删除目录及其内容
                Files.walk(path)
                     .sorted(Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
                return true;
            }
        } catch (Exception e) {
            System.err.println("清理输出目录失败: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 获取输出目录大小
     * @param outputPath 输出目录路径
     * @return 目录大小（字节），-1表示获取失败
     */
    public static long getOutputDirectorySize(String outputPath) {
        if (outputPath == null || outputPath.trim().isEmpty()) {
            return -1;
        }
        
        try {
            Path path = Paths.get(outputPath);
            if (Files.exists(path) && Files.isDirectory(path)) {
                return Files.walk(path)
                           .filter(Files::isRegularFile)
                           .mapToLong(p -> {
                               try {
                                   return Files.size(p);
                               } catch (Exception e) {
                                   return 0;
                               }
                           })
                           .sum();
            }
        } catch (Exception e) {
            System.err.println("获取目录大小失败: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * 获取格式化的目录大小
     * @param outputPath 输出目录路径
     * @return 格式化的大小字符串
     */
    public static String getFormattedOutputDirectorySize(String outputPath) {
        long size = getOutputDirectorySize(outputPath);
        
        if (size < 0) {
            return "未知";
        }
        
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 检查输出目录是否存在
     * @param projectName 项目名称
     * @return 目录是否存在
     */
    public static boolean outputDirectoryExists(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return false;
        }
        
        String cleanProjectName = projectName.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path outputPath = Paths.get("OutputFolder", cleanProjectName);
        
        return Files.exists(outputPath) && Files.isDirectory(outputPath);
    }
    
    /**
     * 获取输出目录路径
     * @param projectName 项目名称
     * @return 输出目录路径
     */
    public static String getOutputDirectoryPath(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return null;
        }
        
        String cleanProjectName = projectName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return Paths.get("OutputFolder", cleanProjectName).toString();
    }
    
    /**
     * 创建打包摘要报告
     * @param result 打包结果
     * @return 摘要报告字符串
     */
    public static String createPackageSummary(PackageResult result) {
        if (result == null) {
            return "无打包结果";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("=== 打包操作摘要 ===\n");
        summary.append("状态: ").append(result.isSuccess() ? "成功" : "失败").append("\n");
        
        if (result.getOutputPath() != null) {
            summary.append("输出路径: ").append(result.getOutputPath()).append("\n");
            summary.append("目录大小: ").append(getFormattedOutputDirectorySize(result.getOutputPath())).append("\n");
        }
        
        summary.append("处理文件数: ").append(result.getProcessedFiles().size()).append("\n");
        
        if (!result.getConflictFiles().isEmpty()) {
            summary.append("文件冲突数: ").append(result.getConflictFiles().size()).append("\n");
        }
        
        if (!result.getErrors().isEmpty()) {
            summary.append("错误数: ").append(result.getErrors().size()).append("\n");
        }
        
        summary.append("耗时: ").append(result.getFormattedDuration()).append("\n");
        
        if (!result.getErrors().isEmpty()) {
            summary.append("\n错误详情:\n");
            for (String error : result.getErrors()) {
                summary.append("  • ").append(error).append("\n");
            }
        }
        
        return summary.toString();
    }
}