package com.aeterhilrin.helpcachemeetpackager.download;

import com.aeterhilrin.helpcachemeetpackager.model.DownloadTask;
import com.aeterhilrin.helpcachemeetpackager.model.FileInfo;
import com.aeterhilrin.helpcachemeetpackager.model.ProjectConfig;
import com.aeterhilrin.helpcachemeetpackager.file.FileRenamer;
import com.aeterhilrin.helpcachemeetpackager.util.ApplicationLogger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下载管理器
 * 负责管理多个下载任务的执行
 * 
 * @author AeterHilrin
 */
public class DownloadManager {
    
    private static final int MAX_CONCURRENT_DOWNLOADS = 3; // 最大并发下载数
    private static final int DOWNLOAD_BUFFER_SIZE = 8192; // 下载缓冲区大小
    private static final String TEMP_DIR = "TempFiles"; // 临时文件目录
    private static final String USER_AGENT = "HelpCacheMeetPackager/1.0.0";
    
    private final ExecutorService downloadExecutor;
    private final List<DownloadTask> tasks;
    private final AtomicInteger completedTasks;
    private DownloadProgressListener progressListener;
    private volatile boolean isCancelled = false;
    
    /**
     * 下载进度监听器接口
     */
    public interface DownloadProgressListener {
        void onTaskStarted(DownloadTask task);
        void onTaskProgress(DownloadTask task, long downloaded, long total);
        void onTaskCompleted(DownloadTask task, FileInfo fileInfo);
        void onTaskFailed(DownloadTask task, String errorMessage);
        void onAllTasksCompleted(List<FileInfo> downloadedFiles);
        void onDownloadCancelled();
    }
    
    public DownloadManager() {
        this.downloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        this.tasks = new ArrayList<>();
        this.completedTasks = new AtomicInteger(0);
        
        // 创建临时目录
        createTempDirectory();
    }
    
    /**
     * 设置下载进度监听器
     */
    public void setProgressListener(DownloadProgressListener listener) {
        this.progressListener = listener;
    }
    
    /**
     * 从项目配置创建下载任务
     * @param config 项目配置
     */
    public void createTasksFromConfig(ProjectConfig config) {
        if (config == null || config.getItems() == null) {
            return;
        }
        
        // 先取消之前的任务（如果有）
        if (!tasks.isEmpty()) {
            ApplicationLogger.logInfo("检测到已存在的任务，清理中...");
            cancelAllDownloads();
        }
        
        tasks.clear();
        completedTasks.set(0);
        isCancelled = false;
        
        ApplicationLogger.logInfo("创建新的下载任务，项目: {}, 任务数: {}", 
            config.getProjectName(), config.getItems().size());
        
        for (ProjectConfig.DownloadItem item : config.getItems()) {
            DownloadTask task = new DownloadTask(
                item.getLanzouUrl(),
                item.getPrefix(),
                item.getSuffix(),
                item.getPassword()
            );
            tasks.add(task);
        }
    }
    
    /**
     * 开始所有下载任务
     */
    public void startAllDownloads() {
        if (tasks.isEmpty()) {
            if (progressListener != null) {
                progressListener.onAllTasksCompleted(new ArrayList<>());
            }
            return;
        }
        
        isCancelled = false;
        completedTasks.set(0);
        
        // 提交所有下载任务
        for (DownloadTask task : tasks) {
            downloadExecutor.submit(() -> executeDownloadTask(task));
        }
    }
    
    /**
     * 执行单个下载任务
     */
    private void executeDownloadTask(DownloadTask task) {
        if (isCancelled) {
            return;
        }
        
        try {
            // 通知任务开始
            if (progressListener != null) {
                progressListener.onTaskStarted(task);
            }
            
            // 解析直链
            task.setStatus(DownloadTask.TaskStatus.PARSING_URL);
            LanzouApiClient.ApiResponse apiResponse = LanzouApiClient.parseDirectUrl(
                task.getOriginalUrl(), task.getPassword()
            );
            
            if (!apiResponse.isSuccess()) {
                task.setStatus(DownloadTask.TaskStatus.FAILED);
                task.setErrorMessage(apiResponse.getErrorMessage());
                
                if (progressListener != null) {
                    progressListener.onTaskFailed(task, apiResponse.getErrorMessage());
                }
                
                checkAllTasksCompleted();
                return;
            }
            
            // 设置解析结果
            task.setDirectUrl(apiResponse.getDirectUrl());
            task.setFileName(apiResponse.getFileName());
            task.setFileSize(apiResponse.getFileSize());
            
            // 开始下载
            task.setStatus(DownloadTask.TaskStatus.DOWNLOADING);
            FileInfo fileInfo = downloadFile(task);
            
            if (fileInfo != null) {
                task.setStatus(DownloadTask.TaskStatus.COMPLETED);
                
                if (progressListener != null) {
                    progressListener.onTaskCompleted(task, fileInfo);
                }
            } else {
                task.setStatus(DownloadTask.TaskStatus.FAILED);
                
                if (progressListener != null) {
                    progressListener.onTaskFailed(task, task.getErrorMessage());
                }
            }
            
        } catch (Exception e) {
            task.setStatus(DownloadTask.TaskStatus.FAILED);
            task.setErrorMessage("下载异常: " + e.getMessage());
            
            if (progressListener != null) {
                progressListener.onTaskFailed(task, e.getMessage());
            }
        }
        
        checkAllTasksCompleted();
    }
    
    /**
     * 下载文件
     */
    private FileInfo downloadFile(DownloadTask task) {
        if (isCancelled) {
            return null;
        }
        
        try {
            URL url = new URL(task.getDirectUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求属性
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "*/*");
            connection.setInstanceFollowRedirects(true);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                task.setErrorMessage("HTTP " + responseCode);
                return null;
            }
            
            // 获取文件大小
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0) {
                task.setFileSize(contentLength);
            }
            
            // 生成文件名
            String fileName = determineFileName(task, connection);
            String filePath = generateFilePath(task, fileName);
            
            // 下载文件
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(filePath);
                 BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
                 BufferedOutputStream bufferedOutput = new BufferedOutputStream(outputStream)) {
                
                byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
                long totalBytesRead = 0;
                int bytesRead;
                
                while ((bytesRead = bufferedInput.read(buffer)) != -1 && !isCancelled) {
                    bufferedOutput.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    task.setDownloadedSize(totalBytesRead);
                    
                    // 通知进度更新
                    if (progressListener != null) {
                        progressListener.onTaskProgress(task, totalBytesRead, task.getFileSize());
                    }
                }
                
                if (isCancelled) {
                    // 删除未完成的文件
                    new File(filePath).delete();
                    return null;
                }
                
                // 使用临时文件名下载，后续会重命名
                String tempFileName = "temp_" + task.getTaskId() + ".zip";
                
                // 创建文件信息对象，使用实际的文件名
                String actualFileName = determineActualFileName(task);
                FileInfo fileInfo = new FileInfo(actualFileName, filePath, task.getPrefix(), task.getSuffix());
                fileInfo.setTaskId(task.getTaskId());
                
                ApplicationLogger.logInfo("下载完成 - 临时文件: {}, 实际文件名: {}", 
                    tempFileName, actualFileName);
                
                return fileInfo;
                
            }
            
        } catch (Exception e) {
            task.setErrorMessage("下载失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 确定文件名
     */
    private String determineFileName(DownloadTask task, HttpURLConnection connection) {
        // 下载阶段使用临时文件名，避免依赖可能不准确的URL文件名
        // 重命名会在下载完成后根据实际内容和配置进行
        String tempFileName = "temp_" + task.getTaskId() + ".zip";
        ApplicationLogger.logInfo("使用临时文件名进行下载: {}", tempFileName);
        return tempFileName;
    }
    
    /**
     * 从Content-Disposition头提取文件名
     */
    private String extractFileNameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null) {
            return null;
        }
        
        // 查找filename=
        int filenameIndex = contentDisposition.toLowerCase().indexOf("filename=");
        if (filenameIndex != -1) {
            String filename = contentDisposition.substring(filenameIndex + 9);
            
            // 移除引号
            if (filename.startsWith("\"") && filename.endsWith("\"")) {
                filename = filename.substring(1, filename.length() - 1);
            }
            
            return filename.trim();
        }
        
        return null;
    }
    
    /**
     * 确定实际的文件名（用于重命名）
     */
    private String determineActualFileName(DownloadTask task) {
        String fileName = null;
        
        // 1. 优先从直链URL中的fileName参数提取文件名
        if (task.getDirectUrl() != null) {
            fileName = extractFileNameFromUrl(task.getDirectUrl());
            if (fileName != null && !fileName.trim().isEmpty()) {
                ApplicationLogger.logInfo("从URL参数提取实际文件名: {}", fileName);
            }
        }
        
        // 2. 使用API返回的文件名
        if (fileName == null || fileName.trim().isEmpty() || "文件".equals(fileName)) {
            // 如果API返回的是通用名称"文件"，尝试使用任务前缀作为文件名
            if ("文件".equals(task.getFileName()) && task.getPrefix() != null) {
                fileName = task.getPrefix() + ".zip";
                ApplicationLogger.logInfo("使用任务前缀作为文件名: {}", fileName);
            } else {
                fileName = task.getFileName();
            }
        }
        
        // 3. 如果仍然为空或是通用名称，使用默认名称
        if (fileName == null || fileName.trim().isEmpty() || "文件".equals(fileName)) {
            fileName = "download_" + task.getTaskId() + ".zip";
            ApplicationLogger.logInfo("使用默认文件名: {}", fileName);
        }
        
        // 4. 确保文件名有扩展名
        if (!fileName.contains(".")) {
            fileName += ".zip";
        }
        
        return sanitizeFileName(fileName);
    }
    private String extractFileNameFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 查找 fileName 参数
            int fileNameIndex = url.indexOf("fileName=");
            if (fileNameIndex != -1) {
                String fileNamePart = url.substring(fileNameIndex + 9); // "fileName=".length() = 9
                
                // 查找参数结束位置（&或字符串结束）
                int endIndex = fileNamePart.indexOf('&');
                if (endIndex != -1) {
                    fileNamePart = fileNamePart.substring(0, endIndex);
                }
                
                // URL解码
                String decodedFileName = java.net.URLDecoder.decode(fileNamePart, "UTF-8");
                ApplicationLogger.logInfo("从URL参数提取文件名: {}", decodedFileName);
                return decodedFileName;
            }
            
        } catch (Exception e) {
            ApplicationLogger.logWarn("提取URL文件名失败: {}", e.getMessage());
        }
        
        return null;
    }
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "download.zip";
        }
        
        // 替换Windows文件名非法字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    
    /**
     * 生成文件路径（下载时使用原始文件名）
     */
    private String generateFilePath(DownloadTask task, String originalFileName) {
        // 下载时使用原始文件名，不添加前缀和后缀
        return Paths.get(TEMP_DIR, originalFileName).toString();
    }
    
    /**
     * 创建临时目录
     */
    private void createTempDirectory() {
        try {
            Path tempDir = Paths.get(TEMP_DIR);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
        } catch (Exception e) {
            System.err.println("创建临时目录失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否所有任务都已完成
     */
    private void checkAllTasksCompleted() {
        int completed = completedTasks.incrementAndGet();
        
        if (completed >= tasks.size()) {
            // 所有任务完成，收集结果并进行文件重命名
            List<FileInfo> downloadedFiles = new ArrayList<>();
            
            for (DownloadTask task : tasks) {
                if (task.isSuccessful()) {
                    // 生成临时文件路径
                    String tempFileName = "temp_" + task.getTaskId() + ".zip";
                    String filePath = generateFilePath(task, tempFileName);
                    
                    if (new File(filePath).exists()) {
                        // 获取实际的文件名（用于重命名）
                        String actualFileName = determineActualFileName(task);
                        
                        // 创建FileInfo对象，使用实际文件名作为原始文件名
                        FileInfo fileInfo = new FileInfo(actualFileName, filePath, 
                            task.getPrefix(), task.getSuffix());
                        fileInfo.setTaskId(task.getTaskId());
                        downloadedFiles.add(fileInfo);
                        
                        ApplicationLogger.logInfo("添加下载成功文件: 临时文件={}, 实际文件名={}", 
                            tempFileName, actualFileName);
                    }
                }
            }
            
            // 执行文件重命名
            if (!downloadedFiles.isEmpty()) {
                renameDownloadedFiles(downloadedFiles);
            } else {
                // 没有成功下载的文件，直接通知完成
                notifyAllTasksCompleted(downloadedFiles);
            }
        }
    }
    
    /**
     * 重命名下载的文件
     */
    private void renameDownloadedFiles(List<FileInfo> downloadedFiles) {
        try {
            int originalDownloadCount = downloadedFiles.size();
            ApplicationLogger.logInfo("开始重命名下载的文件，文件数: {}", originalDownloadCount);
            
            List<FileRenamer.RenameResult> renameResults = FileRenamer.batchRename(
                downloadedFiles, 
                new FileRenamer.BatchRenameListener() {
                    @Override
                    public void onFileRenamed(String oldName, String newName, int currentFile, int totalFiles) {
                        ApplicationLogger.logFileRenamed(oldName, newName);
                    }
                    
                    @Override
                    public void onRenameCompleted(List<FileRenamer.RenameResult> results) {
                        ApplicationLogger.logInfo("文件重命名完成，成功: {}, 失败: {}", 
                            results.stream().mapToInt(r -> r.isSuccess() ? 1 : 0).sum(),
                            results.stream().mapToInt(r -> r.isSuccess() ? 0 : 1).sum());
                    }
                    
                    @Override
                    public void onRenameError(String errorMessage) {
                        ApplicationLogger.logError("文件重命名错误: {}", errorMessage);
                    }
                }
            );
            
            // 更新成功重命名的文件列表
            List<FileInfo> renamedFiles = new ArrayList<>();
            for (int i = 0; i < downloadedFiles.size() && i < renameResults.size(); i++) {
                FileRenamer.RenameResult result = renameResults.get(i);
                FileInfo fileInfo = downloadedFiles.get(i);
                
                if (result.isSuccess()) {
                    // 更新文件信息对象的路径
                    fileInfo.setFilePath(result.getNewPath());
                    renamedFiles.add(fileInfo);
                } else {
                    ApplicationLogger.logFileRenameFailed(
                        fileInfo.getOriginalFileName(), 
                        result.getErrorMessage());
                    // 即使重命名失败，也将文件加入列表（使用原文件名）
                    renamedFiles.add(fileInfo);
                }
            }
            
            // 传递下载成功的文件数量（不是重命名成功数量）
            notifyAllTasksCompleted(renamedFiles);
            
        } catch (Exception e) {
            ApplicationLogger.logException("文件重命名过程", e);
            // 即使重命名失败，也要通知完成
            notifyAllTasksCompleted(downloadedFiles); 
        }
    }
    
    /**
     * 通知所有任务完成
     */
    private void notifyAllTasksCompleted(List<FileInfo> finalFiles) {
        if (progressListener != null) {
            if (isCancelled) {
                progressListener.onDownloadCancelled();
            } else {
                progressListener.onAllTasksCompleted(finalFiles);
            }
        }
    }
    
    /**
     * 取消所有下载任务
     */
    public void cancelAllDownloads() {
        isCancelled = true;
        
        for (DownloadTask task : tasks) {
            if (!task.isFinished()) {
                task.setStatus(DownloadTask.TaskStatus.CANCELLED);
            }
        }
        
        if (progressListener != null) {
            progressListener.onDownloadCancelled();
        }
    }
    
    /**
     * 获取所有任务
     */
    public List<DownloadTask> getTasks() {
        return new ArrayList<>(tasks);
    }
    
    /**
     * 获取任务总数
     */
    public int getTotalTasks() {
        return tasks.size();
    }
    
    /**
     * 获取已完成任务数
     */
    public int getCompletedTasks() {
        return (int) tasks.stream().filter(DownloadTask::isFinished).count();
    }
    
    /**
     * 获取成功完成任务数
     */
    public int getSuccessfulTasks() {
        return (int) tasks.stream().filter(DownloadTask::isSuccessful).count();
    }
    
    /**
     * 检查是否所有任务都已完成
     */
    public boolean isAllTasksCompleted() {
        return tasks.stream().allMatch(DownloadTask::isFinished);
    }
    
    /**
     * 清理临时文件
     */
    public void cleanupTempFiles() {
        try {
            Path tempDir = Paths.get(TEMP_DIR);
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                     .filter(Files::isRegularFile)
                     .forEach(file -> {
                         try {
                             Files.delete(file);
                         } catch (Exception e) {
                             System.err.println("删除临时文件失败: " + file + " - " + e.getMessage());
                         }
                     });
            }
        } catch (Exception e) {
            System.err.println("清理临时文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 关闭下载管理器
     */
    public void shutdown() {
        cancelAllDownloads();
        downloadExecutor.shutdown();
        
        try {
            if (!downloadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                downloadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            downloadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}