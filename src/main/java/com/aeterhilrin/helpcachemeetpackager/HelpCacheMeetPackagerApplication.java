package com.aeterhilrin.helpcachemeetpackager;

import com.aeterhilrin.helpcachemeetpackager.download.DownloadManager;
import com.aeterhilrin.helpcachemeetpackager.download.LanzouApiClient;
import com.aeterhilrin.helpcachemeetpackager.file.PackageManager;
import com.aeterhilrin.helpcachemeetpackager.model.DownloadTask;
import com.aeterhilrin.helpcachemeetpackager.model.FileInfo;
import com.aeterhilrin.helpcachemeetpackager.model.ProjectConfig;
import com.aeterhilrin.helpcachemeetpackager.ui.LicenseDialog;
import com.aeterhilrin.helpcachemeetpackager.ui.MainWindow;
import com.aeterhilrin.helpcachemeetpackager.util.ApplicationLogger;
import com.aeterhilrin.helpcachemeetpackager.util.FileUtils;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * HelpCacheMeetPackager 主应用程序类
 * 应用程序的入口点，负责协调各个组件的工作
 * 
 * @author AeterHilrin
 */
public class HelpCacheMeetPackagerApplication implements MainWindow.MainWindowListener {
    
    private MainWindow mainWindow;
    private DownloadManager downloadManager;
    private ProjectConfig currentConfig;
    private volatile boolean isShuttingDown = false;
    
    /**
     * 应用程序主入口点
     */
    public static void main(String[] args) {
        // 设置系统属性
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        try {
            // 记录应用程序启动
            ApplicationLogger.logApplicationStart();
            
            // 创建并启动应用程序
            HelpCacheMeetPackagerApplication app = new HelpCacheMeetPackagerApplication();
            app.startup();
            
        } catch (Exception e) {
            ApplicationLogger.logException("应用程序启动", e);
            System.err.println("应用程序启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * 应用程序启动
     */
    public void startup() {
        try {
            // 设置Look and Feel
            setupLookAndFeel();
            
            // 显示协议对话框
            if (!showLicenseDialog()) {
                ApplicationLogger.logInfo("用户拒绝软件协议，应用程序退出");
                System.exit(0);
                return;
            }
            
            // 初始化组件
            initializeComponents();
            
            // 显示主窗口
            showMainWindow();
            
            // 设置关闭钩子
            setupShutdownHook();
            
            ApplicationLogger.logInfo("应用程序启动完成");
            
        } catch (Exception e) {
            ApplicationLogger.logException("应用程序启动过程", e);
            showErrorDialog("启动失败", "应用程序启动失败: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * 设置Look and Feel
     */
    private void setupLookAndFeel() {
        try {
            // 设置系统外观和字体
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // 设置默认字体以支持中文
            java.awt.Font font = new java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, 12);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, new javax.swing.plaf.FontUIResource(font));
                }
            }
            
            // 设置一些UI属性
            UIManager.put("OptionPane.yesButtonText", "是");
            UIManager.put("OptionPane.noButtonText", "否");
            UIManager.put("OptionPane.cancelButtonText", "取消");
            UIManager.put("OptionPane.okButtonText", "确定");
            
        } catch (Exception e) {
            ApplicationLogger.logWarn("设置Look and Feel失败: {}", e.getMessage());
        }
    }
    
    /**
     * 显示协议对话框
     */
    private boolean showLicenseDialog() {
        try {
            return LicenseDialog.showLicense(null);
        } catch (Exception e) {
            ApplicationLogger.logException("显示协议对话框", e);
            return false;
        }
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 创建下载管理器
        downloadManager = new DownloadManager();
        downloadManager.setProgressListener(new DownloadProgressHandler());
        
        // 创建主窗口
        mainWindow = new MainWindow();
        mainWindow.setMainWindowListener(this);
        
        ApplicationLogger.logInfo("组件初始化完成");
    }
    
    /**
     * 显示主窗口
     */
    private void showMainWindow() {
        SwingUtilities.invokeLater(() -> {
            mainWindow.setVisible(true);
            
            // 添加窗口关闭监听器
            mainWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    onApplicationExit();
                }
            });
        });
    }
    
    /**
     * 设置关闭钩子
     */
    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!isShuttingDown) {
                ApplicationLogger.logInfo("JVM关闭，执行清理操作");
                cleanup();
            }
        }));
    }
    
    /**
     * 下载进度处理器
     */
    private class DownloadProgressHandler implements DownloadManager.DownloadProgressListener {
        private volatile long lastProgressUpdate = 0;
        private static final long PROGRESS_UPDATE_INTERVAL = 200; // 200毫秒更新一次进度，避免闪烁
        
        @Override
        public void onTaskStarted(DownloadTask task) {
            SwingUtilities.invokeLater(() -> {
                String fileName = task.getFileName() != null ? task.getFileName() : "未知文件";
                String taskDesc = String.format("文件[%s]", fileName);
                mainWindow.updateStatus("开始下载: " + taskDesc);
                ApplicationLogger.logDownloadStart(task.getOriginalUrl(), fileName);
            });
        }
        
        @Override
        public void onTaskProgress(DownloadTask task, long downloaded, long total) {
            // 使用节流机制防止进度条闪烁，特别是大文件下载时
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastProgressUpdate < PROGRESS_UPDATE_INTERVAL) {
                return; // 跳过过于频繁的更新
            }
            lastProgressUpdate = currentTime;
            
            SwingUtilities.invokeLater(() -> {
                int progress = total > 0 ? (int) (downloaded * 100 / total) : 0;
                String fileName = task.getFileName() != null ? task.getFileName() : task.getPrefix();
                String text = String.format("%s: %s/%s", 
                    fileName,
                    FileUtils.formatFileSize(downloaded),
                    total > 0 ? FileUtils.formatFileSize(total) : "未知"
                );
                
                // 使用带缓冲的进度更新，避免连续的UI重绘
                if (!text.equals(mainWindow.getLastProgressText()) || 
                    Math.abs(progress - mainWindow.getLastProgressValue()) >= 2) {
                    mainWindow.updateDownloadProgress(progress, text);
                }
                
                // 每10%记录一次进度
                if (total > 0 && (downloaded * 10 / total) % 1 == 0) {
                    ApplicationLogger.logDownloadProgress(fileName, downloaded, total);
                }
            });
        }
        
        @Override
        public void onTaskCompleted(DownloadTask task, FileInfo fileInfo) {
            SwingUtilities.invokeLater(() -> {
                mainWindow.addFileToList(fileInfo);
                String fileName = task.getFileName() != null ? task.getFileName() : task.getPrefix();
                ApplicationLogger.logDownloadCompleted(
                    fileName, 
                    task.getFileSize(), 
                    System.currentTimeMillis() - task.getStartTime()
                );
            });
        }
        
        @Override
        public void onTaskFailed(DownloadTask task, String errorMessage) {
            SwingUtilities.invokeLater(() -> {
                String fileName = task.getFileName() != null ? task.getFileName() : task.getPrefix();
                mainWindow.updateStatus("下载失败: " + fileName + " - " + errorMessage);
                ApplicationLogger.logDownloadFailed(fileName, errorMessage);
            });
        }
        
        @Override
        public void onAllTasksCompleted(List<FileInfo> downloadedFiles) {
            SwingUtilities.invokeLater(() -> {
                // 使用DownloadManager的实际统计方法
                int successCount = downloadManager.getSuccessfulTasks();
                int totalCount = downloadManager.getTotalTasks();
                
                String message = String.format("下载完成: %d/%d 成功", successCount, totalCount);
                mainWindow.updateStatus(message);
                
                // 只有在有成功下载时才显示100%，否则显示失败状态
                if (successCount > 0) {
                    int progressPercentage = (successCount * 100) / totalCount;
                    mainWindow.updateDownloadProgress(progressPercentage, message);
                } else {
                    mainWindow.updateDownloadProgress(0, "所有下载均失败");
                }
                
                ApplicationLogger.logInfo("所有下载任务完成 - 成功: {}/{}", successCount, totalCount);
                
                // 关键修复：清空文件列表并重新添加重命名后的文件
                // 这样界面显示的就是重命名后的文件信息，而不是临时文件名的信息
                if (successCount > 0 && downloadedFiles != null && !downloadedFiles.isEmpty()) {
                    mainWindow.clearFileList();
                    for (FileInfo renamedFileInfo : downloadedFiles) {
                        mainWindow.addFileToList(renamedFileInfo);
                        ApplicationLogger.logInfo("更新界面文件列表: 文件={}, 路径={}", 
                            renamedFileInfo.getFormattedFileName(), renamedFileInfo.getFilePath());
                    }
                }
                
                if (successCount > 0) {
                    mainWindow.showInfo("下载完成", 
                        String.format("下载完成！\n成功: %d 个文件\n失败: %d 个文件", 
                        successCount, totalCount - successCount));
                } else {
                    mainWindow.showError("下载失败", 
                        String.format("所有下载都失败了！\n总计: %d 个文件\n请检查网络连接和链接有效性。", totalCount));
                }
            });
        }
        
        @Override
        public void onDownloadCancelled() {
            SwingUtilities.invokeLater(() -> {
                mainWindow.updateStatus("下载已取消");
                mainWindow.updateDownloadProgress(0, "已取消");
                ApplicationLogger.logInfo("下载任务被取消");
            });
        }
    }
    
    // 实现 MainWindow.MainWindowListener 接口
    
    @Override
    public void onConfigLoaded(ProjectConfig config) {
        this.currentConfig = config;
        ApplicationLogger.logConfigLoaded("拖拽文件", config.getProjectName(), config.getItems().size());
        
        // 清理临时文件（在加载新配置时）
        cleanupTempFiles();
        
        // 清空之前的文件列表
        mainWindow.clearFileList();
        
        // 测试网络连接
        testNetworkConnection();
    }
    
    @Override
    public void onDownloadRequested(ProjectConfig config) {
        if (config == null || config.getItems() == null || config.getItems().isEmpty()) {
            mainWindow.showError("配置错误", "没有可下载的项目");
            return;
        }
        
        // 清空之前的文件列表
        mainWindow.clearFileList();
        
        // 先清理临时文件
        cleanupTempFiles();
        
        // 清空之前的文件列表
        mainWindow.clearFileList();
        
        // 创建下载任务
        downloadManager.createTasksFromConfig(config);
        
        // 开始下载
        CompletableFuture.runAsync(() -> {
            try {
                downloadManager.startAllDownloads();
            } catch (Exception e) {
                ApplicationLogger.logException("下载过程", e);
                SwingUtilities.invokeLater(() -> {
                    mainWindow.showError("下载错误", "下载过程中发生错误: " + e.getMessage());
                });
            }
        });
        
        ApplicationLogger.logInfo("开始下载任务 - 项目: {}, 任务数: {}", 
            config.getProjectName(), config.getItems().size());
    }
    
    @Override
    public void onPackageRequested(List<FileInfo> selectedFiles, String projectName) {
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            mainWindow.showError("打包错误", "没有选择要打包的文件");
            return;
        }
        
        ApplicationLogger.logPackageStart(projectName, selectedFiles.size());
        
        // 在后台线程执行打包
        CompletableFuture.supplyAsync(() -> {
            return PackageManager.packageFiles(selectedFiles, projectName, 
                new PackageProgressHandler());
        }).thenAccept(result -> {
            SwingUtilities.invokeLater(() -> {
                if (result.isSuccess()) {
                    mainWindow.onPackageCompleted(result.getOutputPath());
                    ApplicationLogger.logPackageCompleted(
                        projectName, 
                        result.getOutputPath(), 
                        result.getProcessedFiles().size(),
                        result.getDuration()
                    );
                } else {
                    String errorMessage = String.join("; ", result.getErrors());
                    mainWindow.onPackageError(errorMessage);
                    ApplicationLogger.logPackageFailed(projectName, errorMessage);
                }
            });
        }).exceptionally(throwable -> {
            SwingUtilities.invokeLater(() -> {
                String errorMessage = "打包过程中发生异常: " + throwable.getMessage();
                mainWindow.onPackageError(errorMessage);
                ApplicationLogger.logPackageFailed(projectName, errorMessage);
            });
            return null;
        });
    }
    
    @Override
    public void onApplicationExit() {
        ApplicationLogger.logInfo("用户请求退出应用程序");
        shutdown();
    }
    
    @Override
    public void onRefreshRequested() {
        // 处理刷新请求，清理临时文件
        cleanupTempFiles();
        ApplicationLogger.logInfo("用户请求刷新，已清理临时文件");
    }
    
    /**
     * 打包进度处理器
     */
    private class PackageProgressHandler implements PackageManager.PackageProgressListener {
        
        @Override
        public void onPackageStarted(String projectName, int totalFiles) {
            SwingUtilities.invokeLater(() -> {
                mainWindow.updateStatus("开始打包: " + projectName);
            });
        }
        
        @Override
        public void onFileProcessing(String fileName, int currentFile, int totalFiles) {
            SwingUtilities.invokeLater(() -> {
                mainWindow.updateStatus(String.format("处理文件: %s (%d/%d)", 
                    fileName, currentFile, totalFiles));
            });
        }
        
        @Override
        public void onFileProcessed(String fileName, int currentFile, int totalFiles) {
            SwingUtilities.invokeLater(() -> {
                int progress = totalFiles > 0 ? (currentFile * 100 / totalFiles) : 0;
                mainWindow.updateDownloadProgress(progress, 
                    String.format("已处理: %d/%d 文件", currentFile, totalFiles));
            });
        }
        
        @Override
        public void onPackageCompleted(String outputPath, PackageManager.PackageResult result) {
            SwingUtilities.invokeLater(() -> {
                mainWindow.updateStatus("打包完成: " + outputPath);
                mainWindow.updateDownloadProgress(100, "打包完成");
            });
        }
        
        @Override
        public void onPackageError(String errorMessage) {
            SwingUtilities.invokeLater(() -> {
                mainWindow.updateStatus("打包失败: " + errorMessage);
            });
        }
        
        @Override
        public void onConflictResolved(String fileName, String action) {
            ApplicationLogger.logFileConflict(fileName, action);
        }
    }
    
    /**
     * 清理临时文件
     */
    public void cleanupTempFiles() {
        if (downloadManager != null) {
            try {
                downloadManager.cleanupTempFiles();
                ApplicationLogger.logInfo("临时文件清理完成");
            } catch (Exception e) {
                ApplicationLogger.logException("清理临时文件", e);
            }
        }
    }
    
    /**
     * 测试网络连接
     */
    private void testNetworkConnection() {
        CompletableFuture.runAsync(() -> {
            boolean apiReachable = LanzouApiClient.testConnection();
            boolean internetReachable = FileUtils.isNetworkReachable("www.baidu.com", 5000);
            
            ApplicationLogger.logNetworkTest("蓝奏云API", apiReachable);
            ApplicationLogger.logNetworkTest("互联网", internetReachable);
            
            if (!internetReachable) {
                SwingUtilities.invokeLater(() -> {
                    mainWindow.updateStatus("网络连接异常，请检查网络设置");
                });
            } else if (!apiReachable) {
                SwingUtilities.invokeLater(() -> {
                    mainWindow.updateStatus("蓝奏云API无法访问，下载功能可能受影响");
                });
            }
        });
    }
    
    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        try {
            isShuttingDown = true;
            
            // 关闭下载管理器
            if (downloadManager != null) {
                downloadManager.shutdown();
            }
            
            // 清理临时文件
            if (downloadManager != null) {
                downloadManager.cleanupTempFiles();
            }
            
            ApplicationLogger.logInfo("资源清理完成");
            
        } catch (Exception e) {
            ApplicationLogger.logException("资源清理", e);
        }
    }
    
    /**
     * 关闭应用程序
     */
    public void shutdown() {
        try {
            ApplicationLogger.logApplicationShutdown();
            
            // 执行清理
            cleanup();
            
            // 关闭主窗口
            if (mainWindow != null) {
                mainWindow.dispose();
            }
            
            // 退出程序
            System.exit(0);
            
        } catch (Exception e) {
            ApplicationLogger.logException("应用程序关闭", e);
            System.exit(1);
        }
    }
}