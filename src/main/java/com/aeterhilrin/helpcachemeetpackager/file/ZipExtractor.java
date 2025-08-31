package com.aeterhilrin.helpcachemeetpackager.file;

import com.aeterhilrin.helpcachemeetpackager.model.FileInfo;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZIP文件解压器
 * 负责解压ZIP文件到指定目录，并处理文件冲突
 * 
 * @author AeterHilrin
 */
public class ZipExtractor {
    
    private static final int BUFFER_SIZE = 8192;
    private static final String OUTPUT_DIR_PREFIX = "OutputFolder";
    
    /**
     * 解压进度监听器接口
     */
    public interface ExtractionProgressListener {
        void onFileExtracted(String fileName, int currentFile, int totalFiles);
        void onExtractionCompleted(String outputPath);
        void onExtractionError(String errorMessage);
        void onFileConflict(String fileName, String action);
    }
    
    /**
     * 解压结果类
     */
    public static class ExtractionResult {
        private final boolean success;
        private final String outputPath;
        private final List<String> extractedFiles;
        private final List<String> conflictFiles;
        private final String errorMessage;
        
        public ExtractionResult(boolean success, String outputPath, 
                              List<String> extractedFiles, List<String> conflictFiles, 
                              String errorMessage) {
            this.success = success;
            this.outputPath = outputPath;
            this.extractedFiles = extractedFiles != null ? new ArrayList<>(extractedFiles) : new ArrayList<>();
            this.conflictFiles = conflictFiles != null ? new ArrayList<>(conflictFiles) : new ArrayList<>();
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public String getOutputPath() { return outputPath; }
        public List<String> getExtractedFiles() { return new ArrayList<>(extractedFiles); }
        public List<String> getConflictFiles() { return new ArrayList<>(conflictFiles); }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            return "ExtractionResult{" +
                    "success=" + success +
                    ", outputPath='" + outputPath + '\'' +
                    ", extractedFiles=" + extractedFiles.size() +
                    ", conflictFiles=" + conflictFiles.size() +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }
    
    /**
     * 批量解压文件到项目目录
     * @param fileInfoList 要解压的文件列表
     * @param projectName 项目名称
     * @param progressListener 进度监听器
     * @return 解压结果
     */
    public static ExtractionResult extractFiles(List<FileInfo> fileInfoList, String projectName, 
                                              ExtractionProgressListener progressListener) {
        if (fileInfoList == null || fileInfoList.isEmpty()) {
            return new ExtractionResult(false, null, null, null, "没有文件需要解压");
        }
        
        if (projectName == null || projectName.trim().isEmpty()) {
            return new ExtractionResult(false, null, null, null, "项目名称不能为空");
        }
        
        try {
            // 创建输出目录
            String outputPath = createOutputDirectory(projectName);
            
            // 按后缀排序文件（确保按顺序解压）
            List<FileInfo> sortedFiles = new ArrayList<>(fileInfoList);
            sortedFiles.sort(Comparator.comparing(FileInfo::getSuffix));
            
            List<String> extractedFiles = new ArrayList<>();
            List<String> conflictFiles = new ArrayList<>();
            
            // 解压每个文件
            for (int i = 0; i < sortedFiles.size(); i++) {
                FileInfo fileInfo = sortedFiles.get(i);
                
                if (!fileInfo.isZipFile()) {
                    // 直接复制非ZIP文件
                    copyNonZipFile(fileInfo, outputPath, extractedFiles);
                } else {
                    // 解压ZIP文件
                    extractSingleZipFile(fileInfo, outputPath, extractedFiles, conflictFiles, 
                                       progressListener, i + 1, sortedFiles.size());
                }
            }
            
            if (progressListener != null) {
                progressListener.onExtractionCompleted(outputPath);
            }
            
            return new ExtractionResult(true, outputPath, extractedFiles, conflictFiles, null);
            
        } catch (Exception e) {
            String errorMessage = "解压过程中发生错误: " + e.getMessage();
            if (progressListener != null) {
                progressListener.onExtractionError(errorMessage);
            }
            return new ExtractionResult(false, null, null, null, errorMessage);
        }
    }
    
    /**
     * 解压单个ZIP文件
     */
    private static void extractSingleZipFile(FileInfo fileInfo, String outputPath,
                                           List<String> extractedFiles, List<String> conflictFiles,
                                           ExtractionProgressListener progressListener,
                                           int currentFile, int totalFiles) throws IOException {
        
        File zipFile = new File(fileInfo.getFilePath());
        if (!zipFile.exists()) {
            throw new IOException("ZIP文件不存在: " + fileInfo.getFilePath());
        }
        
        // 尝试不同的字符编码
        Charset[] charsets = {StandardCharsets.UTF_8, Charset.forName("GBK"), Charset.forName("GB2312")};
        
        for (Charset charset : charsets) {
            try {
                extractWithCharset(zipFile, outputPath, charset, extractedFiles, conflictFiles, 
                                 progressListener, currentFile, totalFiles, fileInfo.getSuffix());
                return; // 成功解压，退出
            } catch (Exception e) {
                // 尝试下一个字符编码
                if (charset == charsets[charsets.length - 1]) {
                    // 最后一个字符编码也失败了，抛出异常
                    throw new IOException("无法解压文件: " + fileInfo.getOriginalFileName() + 
                                        " - " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 使用指定字符编码解压ZIP文件
     */
    private static void extractWithCharset(File zipFile, String outputPath, Charset charset,
                                         List<String> extractedFiles, List<String> conflictFiles,
                                         ExtractionProgressListener progressListener,
                                         int currentFile, int totalFiles, int fileSuffix) throws IOException {
        
        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis, charset)) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    // 创建目录
                    createDirectory(outputPath, entry.getName());
                } else {
                    // 解压文件
                    extractFileEntry(zis, entry, outputPath, extractedFiles, conflictFiles,
                                   progressListener, currentFile, totalFiles, fileSuffix);
                }
                zis.closeEntry();
            }
        }
    }
    
    /**
     * 解压单个文件条目
     */
    private static void extractFileEntry(ZipInputStream zis, ZipEntry entry, String outputPath,
                                       List<String> extractedFiles, List<String> conflictFiles,
                                       ExtractionProgressListener progressListener,
                                       int currentFile, int totalFiles, int fileSuffix) throws IOException {
        
        String fileName = entry.getName();
        Path targetPath = Paths.get(outputPath, fileName);
        
        // 创建父目录
        Files.createDirectories(targetPath.getParent());
        
        boolean shouldExtract = true;
        
        // 检查文件冲突
        if (Files.exists(targetPath)) {
            shouldExtract = handleFileConflict(targetPath, fileSuffix, conflictFiles, progressListener);
        }
        
        if (shouldExtract) {
            // 解压文件
            try (FileOutputStream fos = new FileOutputStream(targetPath.toFile());
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = zis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
            }
            
            extractedFiles.add(fileName);
            
            if (progressListener != null) {
                progressListener.onFileExtracted(fileName, currentFile, totalFiles);
            }
        }
    }
    
    /**
     * 处理文件冲突
     * @param targetPath 目标文件路径
     * @param currentFileSuffix 当前文件的后缀
     * @param conflictFiles 冲突文件列表
     * @param progressListener 进度监听器
     * @return 是否应该解压（覆盖）文件
     */
    private static boolean handleFileConflict(Path targetPath, int currentFileSuffix,
                                            List<String> conflictFiles, 
                                            ExtractionProgressListener progressListener) {
        
        String fileName = targetPath.getFileName().toString();
        
        // 检查现有文件是否来自后缀更小的文件
        // 如果是，则用后缀更大的文件覆盖（根据设计文档要求）
        
        try {
            // 简单策略：总是覆盖，因为文件是按后缀排序的
            // 后解压的文件（后缀更大）应该覆盖先解压的文件
            conflictFiles.add(fileName);
            
            if (progressListener != null) {
                progressListener.onFileConflict(fileName, "覆盖 (后缀 " + currentFileSuffix + ")");
            }
            
            return true; // 覆盖文件
            
        } catch (Exception e) {
            // 出现异常时不覆盖
            return false;
        }
    }
    
    /**
     * 复制非ZIP文件
     */
    private static void copyNonZipFile(FileInfo fileInfo, String outputPath, 
                                     List<String> extractedFiles) throws IOException {
        
        Path sourcePath = Paths.get(fileInfo.getFilePath());
        Path targetPath = Paths.get(outputPath, fileInfo.getOriginalFileName());
        
        // 创建父目录
        Files.createDirectories(targetPath.getParent());
        
        // 复制文件
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        extractedFiles.add(fileInfo.getOriginalFileName());
    }
    
    /**
     * 创建输出目录
     */
    private static String createOutputDirectory(String projectName) throws IOException {
        // 清理项目名称中的非法字符
        String cleanProjectName = projectName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        Path outputBasePath = Paths.get(OUTPUT_DIR_PREFIX);
        Path outputPath = outputBasePath.resolve(cleanProjectName);
        
        // 创建目录
        Files.createDirectories(outputPath);
        
        return outputPath.toString();
    }
    
    /**
     * 创建目录
     */
    private static void createDirectory(String basePath, String dirName) throws IOException {
        Path dirPath = Paths.get(basePath, dirName);
        Files.createDirectories(dirPath);
    }
    
    /**
     * 验证ZIP文件
     * @param filePath ZIP文件路径
     * @return 验证结果信息
     */
    public static String validateZipFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return "文件路径为空";
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            return "文件不存在";
        }
        
        if (!file.isFile()) {
            return "不是文件";
        }
        
        if (file.length() == 0) {
            return "文件为空";
        }
        
        // 尝试读取ZIP文件头
        try (FileInputStream fis = new FileInputStream(file);
             ZipInputStream zis = new ZipInputStream(fis)) {
            
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                return "ZIP文件为空或损坏";
            }
            
            return null; // 验证通过
            
        } catch (Exception e) {
            return "ZIP文件格式错误: " + e.getMessage();
        }
    }
    
    /**
     * 获取ZIP文件信息
     * @param filePath ZIP文件路径
     * @return ZIP文件信息
     */
    public static ZipFileInfo getZipFileInfo(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             ZipInputStream zis = new ZipInputStream(fis, StandardCharsets.UTF_8)) {
            
            List<String> fileNames = new ArrayList<>();
            List<String> directoryNames = new ArrayList<>();
            long totalSize = 0;
            int fileCount = 0;
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    directoryNames.add(entry.getName());
                } else {
                    fileNames.add(entry.getName());
                    totalSize += entry.getSize();
                    fileCount++;
                }
                zis.closeEntry();
            }
            
            return new ZipFileInfo(true, fileNames, directoryNames, totalSize, fileCount, null);
            
        } catch (Exception e) {
            return new ZipFileInfo(false, null, null, 0, 0, e.getMessage());
        }
    }
    
    /**
     * ZIP文件信息类
     */
    public static class ZipFileInfo {
        private final boolean valid;
        private final List<String> fileNames;
        private final List<String> directoryNames;
        private final long totalSize;
        private final int fileCount;
        private final String errorMessage;
        
        public ZipFileInfo(boolean valid, List<String> fileNames, List<String> directoryNames,
                          long totalSize, int fileCount, String errorMessage) {
            this.valid = valid;
            this.fileNames = fileNames != null ? new ArrayList<>(fileNames) : new ArrayList<>();
            this.directoryNames = directoryNames != null ? new ArrayList<>(directoryNames) : new ArrayList<>();
            this.totalSize = totalSize;
            this.fileCount = fileCount;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getFileNames() { return new ArrayList<>(fileNames); }
        public List<String> getDirectoryNames() { return new ArrayList<>(directoryNames); }
        public long getTotalSize() { return totalSize; }
        public int getFileCount() { return fileCount; }
        public String getErrorMessage() { return errorMessage; }
        
        public String getFormattedTotalSize() {
            if (totalSize < 1024) {
                return totalSize + " B";
            } else if (totalSize < 1024 * 1024) {
                return String.format("%.2f KB", totalSize / 1024.0);
            } else if (totalSize < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", totalSize / (1024.0 * 1024.0));
            } else {
                return String.format("%.2f GB", totalSize / (1024.0 * 1024.0 * 1024.0));
            }
        }
        
        @Override
        public String toString() {
            return "ZipFileInfo{" +
                    "valid=" + valid +
                    ", fileCount=" + fileCount +
                    ", totalSize=" + getFormattedTotalSize() +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }
}