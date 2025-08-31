package com.aeterhilrin.helpcachemeetpackager.ui;

import com.aeterhilrin.helpcachemeetpackager.config.YamlParser;
import com.aeterhilrin.helpcachemeetpackager.config.ConfigValidator;
import com.aeterhilrin.helpcachemeetpackager.model.ProjectConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/**
 * 左侧配置文件拖拽面板
 * 用于拖拽导入YAML配置文件
 * 
 * @author AeterHilrin
 */
public class LeftPanel extends JPanel implements DropTargetListener {
    
    private JLabel dropLabel;
    private JButton refreshButton;
    private ProjectConfig currentConfig;
    private ConfigLoadListener configLoadListener;
    
    // 配置加载监听器接口
    public interface ConfigLoadListener {
        void onConfigLoaded(ProjectConfig config);
        void onConfigLoadError(String errorMessage);
        void onRefreshRequested(); // 新增：刷新请求回调
    }
    
    public LeftPanel() {
        initializeComponents();
        setupLayout();
        setupDragAndDrop();
        setupEventHandlers();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        setBorder(new TitledBorder("配置文件"));
        setPreferredSize(new Dimension(250, 400));
        
        // 拖拽提示标签
        dropLabel = new JLabel("<html><div style='text-align: center;'>" +
                "请将配置文件<br/>拖拽至此处<br/><br/>" +
                "<small>支持 .yaml 和 .yml 文件</small>" +
                "</div></html>");
        dropLabel.setHorizontalAlignment(JLabel.CENTER);
        dropLabel.setVerticalAlignment(JLabel.CENTER);
        dropLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        dropLabel.setForeground(Color.GRAY);
        
        // 设置拖拽区域样式
        dropLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(Color.GRAY, 2, 5, 5, false),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        dropLabel.setOpaque(true);
        dropLabel.setBackground(new Color(250, 250, 250));
        
        // 刷新按钮
        refreshButton = new JButton("刷新");
        refreshButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        refreshButton.setPreferredSize(new Dimension(100, 30));
        refreshButton.setEnabled(false);
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 主拖拽区域
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(dropLabel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        
        // 底部按钮区域
        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.add(refreshButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 设置拖拽功能
     */
    private void setupDragAndDrop() {
        // 创建拖拽目标
        DropTarget dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY, this, true);
        setDropTarget(dropTarget);
        
        // 为拖拽标签也设置拖拽目标
        dropLabel.setDropTarget(new DropTarget(dropLabel, DnDConstants.ACTION_COPY, this, true));
    }
    
    /**
     * 设置事件处理
     */
    private void setupEventHandlers() {
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshConfig();
            }
        });
    }
    
    /**
     * 设置配置加载监听器
     */
    public void setConfigLoadListener(ConfigLoadListener listener) {
        this.configLoadListener = listener;
    }
    
    /**
     * 刷新配置
     */
    private void refreshConfig() {
        // 先通知监听器清理临时文件
        if (configLoadListener != null) {
            configLoadListener.onRefreshRequested();
        }
        
        if (currentConfig != null && configLoadListener != null) {
            configLoadListener.onConfigLoaded(currentConfig);
            showSuccess("配置已刷新");
        }
    }
    
    /**
     * 处理拖拽文件
     */
    private void handleDroppedFiles(List<File> files) {
        if (files.isEmpty()) {
            showError("没有检测到文件");
            return;
        }
        
        File file = files.get(0); // 只处理第一个文件
        
        if (!isValidConfigFile(file)) {
            showError("请拖拽有效的配置文件 (.yaml 或 .yml)");
            return;
        }
        
        // 在加载新配置文件之前，先清理临时文件
        if (configLoadListener != null) {
            configLoadListener.onRefreshRequested();
        }
        
        try {
            loadConfigFile(file);
        } catch (Exception e) {
            showError("加载配置文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证是否为有效的配置文件
     */
    private boolean isValidConfigFile(File file) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfigFile(File file) {
        // 显示加载状态
        showLoading("正在加载配置文件...");
        
        // 在后台线程中加载配置文件
        SwingWorker<ProjectConfig, Void> worker = new SwingWorker<ProjectConfig, Void>() {
            @Override
            protected ProjectConfig doInBackground() throws Exception {
                // 验证配置文件
                ConfigValidator.ValidationResult validationResult = ConfigValidator.validateConfigFile(file);
                if (!validationResult.isValid()) {
                    throw new Exception("配置文件验证失败:\n" + validationResult.getFormattedMessage());
                }
                
                // 解析配置文件
                ProjectConfig config = YamlParser.parseFromFile(file);
                
                if (!config.isValid()) {
                    throw new Exception("配置文件内容无效");
                }
                
                return config;
            }
            
            @Override
            protected void done() {
                try {
                    ProjectConfig config = get();
                    currentConfig = config;
                    
                    if (configLoadListener != null) {
                        configLoadListener.onConfigLoaded(config);
                    }
                    
                    showSuccess("配置文件加载成功\n项目: " + config.getProjectName());
                    refreshButton.setEnabled(true);
                    
                } catch (Exception e) {
                    showError("加载配置文件失败: " + e.getMessage());
                    if (configLoadListener != null) {
                        configLoadListener.onConfigLoadError(e.getMessage());
                    }
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * 显示加载状态
     */
    private void showLoading(String message) {
        dropLabel.setText("<html><div style='text-align: center;'>" +
                "<b>加载中...</b><br/><br/>" +
                message +
                "</div></html>");
        dropLabel.setForeground(Color.BLUE);
    }
    
    /**
     * 显示成功信息
     */
    private void showSuccess(String message) {
        dropLabel.setText("<html><div style='text-align: center;'>" +
                "<b>✓ 成功</b><br/><br/>" +
                message.replace("\n", "<br/>") +
                "</div></html>");
        dropLabel.setForeground(new Color(0, 128, 0));
    }
    
    /**
     * 显示错误信息
     */
    private void showError(String message) {
        dropLabel.setText("<html><div style='text-align: center;'>" +
                "<b>✗ 错误</b><br/><br/>" +
                message.replace("\n", "<br/>") +
                "</div></html>");
        dropLabel.setForeground(Color.RED);
        
        // 3秒后恢复初始状态
        Timer timer = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetToInitialState();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    /**
     * 重置到初始状态
     */
    private void resetToInitialState() {
        dropLabel.setText("<html><div style='text-align: center;'>" +
                "请将配置文件<br/>拖拽至此处<br/><br/>" +
                "<small>支持 .yaml 和 .yml 文件</small>" +
                "</div></html>");
        dropLabel.setForeground(Color.GRAY);
    }
    
    // 实现 DropTargetListener 接口
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        dropLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(Color.BLUE, 2, 5, 5, false),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        dropLabel.setBackground(new Color(230, 240, 255));
    }
    
    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        // 保持拖拽状态
    }
    
    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // 拖拽动作改变时的处理
    }
    
    @Override
    public void dragExit(DropTargetEvent dte) {
        dropLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(Color.GRAY, 2, 5, 5, false),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        dropLabel.setBackground(new Color(250, 250, 250));
    }
    
    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            dtde.acceptDrop(DnDConstants.ACTION_COPY);
            Transferable transferable = dtde.getTransferable();
            
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                handleDroppedFiles(files);
            }
            
            dtde.dropComplete(true);
        } catch (Exception e) {
            showError("拖拽文件处理失败: " + e.getMessage());
            dtde.dropComplete(false);
        }
        
        // 恢复正常样式
        dragExit(null);
    }
    
    /**
     * 获取当前配置
     */
    public ProjectConfig getCurrentConfig() {
        return currentConfig;
    }
    
    /**
     * 清除当前配置
     */
    public void clearConfig() {
        currentConfig = null;
        refreshButton.setEnabled(false);
        resetToInitialState();
    }
}