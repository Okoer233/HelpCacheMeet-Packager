package com.aeterhilrin.helpcachemeetpackager.ui;

import com.aeterhilrin.helpcachemeetpackager.model.FileInfo;
import com.aeterhilrin.helpcachemeetpackager.model.ProjectConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * 主窗口界面
 * 应用程序的主界面，包含左右两个面板
 * 
 * @author AeterHilrin
 */
public class MainWindow extends JFrame implements LeftPanel.ConfigLoadListener, RightPanel.PackageListener {
    
    private LeftPanel leftPanel;
    private RightPanel rightPanel;
    private JMenuBar menuBar;
    private MainWindowListener mainWindowListener;
    
    // 进度缓存变量，用于防止进度条闪烁
    private volatile String lastProgressText = "";
    private volatile int lastProgressValue = -1;
    
    // 主窗口监听器接口
    public interface MainWindowListener {
        void onConfigLoaded(ProjectConfig config);
        void onDownloadRequested(ProjectConfig config);
        void onPackageRequested(List<FileInfo> selectedFiles, String projectName);
        void onApplicationExit();
        void onRefreshRequested(); // 新增：刷新请求回调
    }
    
    public MainWindow() {
        initializeComponents();
        setupLayout();
        setupMenuBar();
        setupEventHandlers();
        setupWindow();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        setTitle("HelpCacheMeetPackager - 哈基咪打包器");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // 创建左右面板
        leftPanel = new LeftPanel();
        rightPanel = new RightPanel();
        
        // 设置监听器
        leftPanel.setConfigLoadListener(this);
        rightPanel.setPackageListener(this);
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.3);
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
        
        // 状态栏
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        
        JLabel statusLabel = new JLabel("就绪");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        statusBar.add(statusLabel, BorderLayout.WEST);
        
        JLabel versionLabel = new JLabel("v1.0.0 by AeterHilrin");//先定成1.0.0，有bug再改
        versionLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        versionLabel.setForeground(Color.GRAY);
        statusBar.add(versionLabel, BorderLayout.EAST);
        
        add(statusBar, BorderLayout.SOUTH);
    }
    
    /**
     * 设置菜单栏
     */
    private void setupMenuBar() {
        menuBar = new JMenuBar();
        
        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        fileMenu.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        JMenuItem openConfigItem = new JMenuItem("打开配置文件...");
        openConfigItem.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        openConfigItem.addActionListener(e -> openConfigFile());
        
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        exitItem.addActionListener(e -> exitApplication());
        
        fileMenu.add(openConfigItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        helpMenu.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        aboutItem.addActionListener(e -> showAbout());
        
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    /**
     * 设置事件处理
     */
    private void setupEventHandlers() {
        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
    }
    
    /**
     * 设置窗口属性
     */
    private void setupWindow() {
        // 设置图标
        try {
            setIconImage(Toolkit.getDefaultToolkit().createImage("icon.png"));
        } catch (Exception e) {
            // 忽略图标加载失败
        }
        
        // 设置大小和位置
        setSize(800, 600);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);
        
        // 设置Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            // 忽略Look and Feel设置失败
        }
    }
    
    /**
     * 设置主窗口监听器
     */
    public void setMainWindowListener(MainWindowListener listener) {
        this.mainWindowListener = listener;
    }
    
    /**
     * 打开配置文件
     */
    private void openConfigFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择配置文件");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                return f.isDirectory() || 
                       f.getName().toLowerCase().endsWith(".yaml") || 
                       f.getName().toLowerCase().endsWith(".yml");
            }
            
            @Override
            public String getDescription() {
                return "YAML配置文件 (*.yaml, *.yml)";
            }
        });
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            // TODO: 处理选择的配置文件
            JOptionPane.showMessageDialog(this, 
                "配置文件选择功能待实现", 
                "提示", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * 显示关于对话框
     */
    private void showAbout() {
        String aboutText = 
                "HelpCacheMeetPackager v1.0.0\n\n" +
                "哈基咪打包器\n\n" +
                "作者: AeterHilrin\n" +
                "QQ: 你猜\n\n" +
                "基于MIT协议开源\n" +
                "使用Java 8 + Swing开发";
        
        JOptionPane.showMessageDialog(this,
                aboutText,
                "关于 HelpCacheMeetPackager",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * 退出应用程序
     */
    private void exitApplication() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要退出程序吗？",
                "确认退出",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            if (mainWindowListener != null) {
                mainWindowListener.onApplicationExit();
            }
            System.exit(0);
        }
    }
    
    /**
     * 添加文件到右侧面板
     */
    public void addFileToList(FileInfo fileInfo) {
        rightPanel.addFile(fileInfo);
    }
    
    /**
     * 更新下载进度
     */
    public void updateDownloadProgress(int value, String text) {
        // 更新缓存值
        lastProgressValue = value;
        lastProgressText = text;
        rightPanel.updateProgress(value, text);
    }
    
    /**
     * 获取上次进度文本（用于防止闪烁）
     */
    public String getLastProgressText() {
        return lastProgressText;
    }
    
    /**
     * 获取上次进度值（用于防止闪烁）
     */
    public int getLastProgressValue() {
        return lastProgressValue;
    }
    
    /**
     * 更新状态信息
     */
    public void updateStatus(String status) {
        rightPanel.updateStatus(status);
    }
    
    /**
     * 清空文件列表
     */
    public void clearFileList() {
        rightPanel.clearFiles();
    }
    
    /**
     * 显示错误消息
     */
    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * 显示信息消息
     */
    public void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    // 实现 LeftPanel.ConfigLoadListener 接口
    @Override
    public void onConfigLoaded(ProjectConfig config) {
        rightPanel.setCurrentConfig(config);
        
        if (mainWindowListener != null) {
            mainWindowListener.onConfigLoaded(config);
            mainWindowListener.onDownloadRequested(config);
        }
    }
    
    @Override
    public void onRefreshRequested() {
        // 通知主应用程序清理临时文件
        if (mainWindowListener != null) {
            mainWindowListener.onRefreshRequested();
        }
    }
    
    @Override
    public void onConfigLoadError(String errorMessage) {
        showError("配置加载错误", errorMessage);
    }
    
    // 实现 RightPanel.PackageListener 接口
    @Override
    public void onPackageRequested(List<FileInfo> selectedFiles, String projectName) {
        if (mainWindowListener != null) {
            mainWindowListener.onPackageRequested(selectedFiles, projectName);
        }
    }
    
    @Override
    public void onPackageCompleted(String outputPath) {
        rightPanel.onPackageCompleted(outputPath);
    }
    
    @Override
    public void onPackageError(String errorMessage) {
        rightPanel.onPackageError(errorMessage);
    }
}