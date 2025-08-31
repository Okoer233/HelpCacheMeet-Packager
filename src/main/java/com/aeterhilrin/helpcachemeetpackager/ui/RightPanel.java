package com.aeterhilrin.helpcachemeetpackager.ui;

import com.aeterhilrin.helpcachemeetpackager.model.FileInfo;
import com.aeterhilrin.helpcachemeetpackager.model.ProjectConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 右侧文件选择和打包面板
 * 显示下载完成的文件列表并提供打包功能
 * 
 * @author AeterHilrin
 */
public class RightPanel extends JPanel {
    
    private DefaultListModel<FileInfo> fileListModel;
    private JList<FileInfo> fileList;
    private JProgressBar progressBar;
    private JButton packageButton;
    private JLabel statusLabel;
    private ProjectConfig currentConfig;
    private PackageListener packageListener;
    
    // 打包监听器接口
    public interface PackageListener {
        void onPackageRequested(List<FileInfo> selectedFiles, String projectName);
        void onPackageCompleted(String outputPath);
        void onPackageError(String errorMessage);
    }
    
    public RightPanel() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        setBorder(new TitledBorder("文件选择区域"));
        setPreferredSize(new Dimension(400, 400));
        
        // 文件列表模型和组件
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer(new FileInfoCellRenderer());
        fileList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        // 设置列表外观
        fileList.setFixedCellHeight(-1); // 自动高度
        fileList.setBackground(Color.WHITE);
        fileList.setSelectionBackground(new Color(0, 123, 255, 30)); // 半透明选中背景
        fileList.setSelectionForeground(Color.BLACK);
        fileList.setBorder(null);
        fileList.setFocusable(true);
        
        // 进度条
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("等待下载...");
        progressBar.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        progressBar.setPreferredSize(new Dimension(0, 22));
        
        // 现代化进度条外观
        progressBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 1),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        
        // 打包按钮
        packageButton = new JButton("打包");
        packageButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        packageButton.setPreferredSize(new Dimension(120, 36));
        packageButton.setEnabled(false);
        
        // 现代化按钮外观 - 添加多层保护防止颜色变化
        packageButton.setBackground(new Color(0, 123, 255));
        packageButton.setForeground(Color.WHITE);
        packageButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 1),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        
        // 多层保护：确保按钮样式不被其他操作影响，完全防止高亮
        packageButton.setOpaque(true);
        packageButton.setFocusPainted(false);
        packageButton.setContentAreaFilled(true);
        packageButton.setBorderPainted(true);
        packageButton.setRolloverEnabled(false); // 禁用鼠标悬停效果
        packageButton.setRequestFocusEnabled(false); // 禁用焦点获取
        packageButton.setFocusable(false); // 完全禁用焦点
        packageButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 状态标签
        statusLabel = new JLabel("等待配置文件...");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(108, 117, 125));
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 顶部区域：文件列表标题和操作按钮
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("文件列表");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        titleLabel.setForeground(new Color(51, 51, 51));
        
        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        
        JButton selectAllButton = new JButton("全选");
        selectAllButton.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        selectAllButton.setPreferredSize(new Dimension(50, 24));
        selectAllButton.setBackground(Color.WHITE);
        selectAllButton.setForeground(new Color(0, 123, 255));
        selectAllButton.setBorder(BorderFactory.createLineBorder(new Color(0, 123, 255), 1));
        selectAllButton.setFocusPainted(false);
        selectAllButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selectAllButton.addActionListener(e -> toggleSelectAll());
        
        buttonGroup.add(selectAllButton);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(buttonGroup, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // 文件列表区域
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(380, 250));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230), 1));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // 美化滚动条
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(0, 123, 255, 100);
                this.trackColor = new Color(248, 249, 250);
            }
        });
        
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
        listPanel.add(scrollPane, BorderLayout.CENTER);
        add(listPanel, BorderLayout.CENTER);
        
        // 底部控制区域
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        // 进度条区域
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // 按钮和状态区域
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        // 状态标签
        controlPanel.add(statusLabel, BorderLayout.WEST);
        
        // 按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // 更新打包按钮样式
        updatePackageButton();
        buttonPanel.add(packageButton);
        
        controlPanel.add(buttonPanel, BorderLayout.EAST);
        
        bottomPanel.add(progressPanel, BorderLayout.NORTH);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 设置事件处理
     */
    private void setupEventHandlers() {
        packageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performPackaging();
            }
        });
        
        // 注意：移除了fileList.addListSelectionListener，因为JList的选择与业务逻辑的文件选中是两个概念
        // JList的选择只是UI高亮显示，真正的文件选中通过FileInfo.setSelected()控制
        
        // 优化的鼠标交互：双击切换选中状态
        fileList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int index = fileList.locationToIndex(e.getPoint());
                if (index >= 0 && index < fileListModel.getSize()) {
                    FileInfo fileInfo = fileListModel.getElementAt(index);
                    
                    // 必须连续双击才能选中文件
                    if (e.getClickCount() == 2) {
                        fileInfo.setSelected(!fileInfo.isSelected());
                        fileList.repaint();
                        updatePackageButtonState();
                        
                        // 更新状态显示
                        int selectedCount = getSelectedFiles().size();
                        updateStatus(String.format("已选中 %d 个文件", selectedCount));
                    }
                }
            }
            
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                fileList.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                fileList.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        
        // 键盘快捷键支持
        fileList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    int selectedIndex = fileList.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        FileInfo fileInfo = fileListModel.getElementAt(selectedIndex);
                        fileInfo.setSelected(!fileInfo.isSelected());
                        fileList.repaint();
                        updatePackageButtonState();
                        
                        // 更新状态显示
                        int selectedCount = getSelectedFiles().size();
                        updateStatus(String.format("已选中 %d 个文件", selectedCount));
                        
                        e.consume(); // 阻止默认处理
                    }
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_A && e.isControlDown()) {
                    // Ctrl+A 全选
                    toggleSelectAll();
                    e.consume();
                }
            }
        });
    }
    
    /**
     * 设置打包监听器
     */
    public void setPackageListener(PackageListener listener) {
        this.packageListener = listener;
    }
    
    /**
     * 设置当前配置
     */
    public void setCurrentConfig(ProjectConfig config) {
        this.currentConfig = config;
        updatePackageButton();
        updateStatus("配置已加载: " + (config != null ? config.getProjectName() : "无"));
    }
    
    /**
     * 更新打包按钮外观
     * 只在配置变化时调用，确保按钮样式的一致性
     */
    private void updatePackageButton() {
        if (currentConfig != null && currentConfig.getProjectName() != null) {
            packageButton.setText(currentConfig.getProjectName() + " | 打包");
        } else {
            packageButton.setText("打包");
        }
        
        // 设置按钮的固定样式，只在配置变化时更新
        SwingUtilities.invokeLater(() -> {
            boolean hasConfig = currentConfig != null;
            Color bgColor, fgColor;
            
            if (hasConfig) {
                bgColor = new Color(0, 123, 255);
                fgColor = Color.WHITE;
            } else {
                bgColor = new Color(108, 117, 125);
                fgColor = Color.WHITE;
            }
            
            // 强制设置颜色并锁定
            packageButton.setBackground(bgColor);
            packageButton.setForeground(fgColor);
            
            // 重新应用边框样式
            packageButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
            ));
            
            // 确保按钮不透明，防止颜色被父组件影响
            packageButton.setOpaque(true);
            
            // 禁用按钮的focus绘制，防止颜色被覆盖
            packageButton.setFocusPainted(false);
            
            // 完全禁用高亮效果，防止按钮被高亮
            packageButton.setContentAreaFilled(true);
            packageButton.setBorderPainted(true);
            packageButton.setRolloverEnabled(false); // 禁用鼠标悬停效果
            packageButton.setRequestFocusEnabled(false); // 禁用焦点获取
            packageButton.setFocusable(false); // 完全禁用焦点
            
            packageButton.repaint();
        });
        
        updatePackageButtonState();
    }
    
    /**
     * 更新打包按钮状态
     * 严格只更新启用状态，绝不改变按钮外观
     */
    private void updatePackageButtonState() {
        boolean hasSelectedFiles = getSelectedFiles().size() > 0;
        boolean hasConfig = currentConfig != null;
        boolean shouldEnable = hasSelectedFiles && hasConfig;
        
        // 只更新启用状态，完全不触及外观
        if (packageButton.isEnabled() != shouldEnable) {
            packageButton.setEnabled(shouldEnable);
        }
    }
    
    /**
     * 添加文件到列表
     */
    public void addFile(FileInfo fileInfo) {
        SwingUtilities.invokeLater(() -> {
            fileListModel.addElement(fileInfo);
            updatePackageButtonState();
            updateStatus("文件已添加: " + fileInfo.getFormattedFileName());
        });
    }
    
    /**
     * 移除文件
     */
    public void removeFile(FileInfo fileInfo) {
        SwingUtilities.invokeLater(() -> {
            fileListModel.removeElement(fileInfo);
            updatePackageButtonState();
        });
    }
    
    /**
     * 清空文件列表
     */
    public void clearFiles() {
        SwingUtilities.invokeLater(() -> {
            fileListModel.clear();
            updatePackageButtonState();
            updateStatus("文件列表已清空");
        });
    }
    
    /**
     * 切换全选/取消全选
     */
    private void toggleSelectAll() {
        if (fileListModel.getSize() == 0) {
            return;
        }
        
        // 检查是否全部已选中
        boolean allSelected = true;
        for (int i = 0; i < fileListModel.getSize(); i++) {
            if (!fileListModel.getElementAt(i).isSelected()) {
                allSelected = false;
                break;
            }
        }
        
        // 如果全部已选中，则取消全选；否则全选
        for (int i = 0; i < fileListModel.getSize(); i++) {
            fileListModel.getElementAt(i).setSelected(!allSelected);
        }
        
        fileList.repaint();
        updatePackageButtonState();
        
        // 更新状态显示
        int selectedCount = getSelectedFiles().size();
        String statusText = allSelected ? "已取消全选" : String.format("已全选 %d 个文件", selectedCount);
        updateStatus(statusText);
    }
    
    /**
     * 获取选中的文件
     */
    public List<FileInfo> getSelectedFiles() {
        List<FileInfo> selectedFiles = new ArrayList<>();
        for (int i = 0; i < fileListModel.getSize(); i++) {
            FileInfo fileInfo = fileListModel.getElementAt(i);
            if (fileInfo.isSelected()) {
                selectedFiles.add(fileInfo);
            }
        }
        return selectedFiles;
    }
    
    /**
     * 更新下载进度
     */
    public void updateProgress(int value, String text) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            progressBar.setString(text);
        });
    }
    
    /**
     * 更新状态信息
     */
    public void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
        });
    }
    
    /**
     * 执行打包操作
     */
    private void performPackaging() {
        List<FileInfo> selectedFiles = getSelectedFiles();
        
        if (selectedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "请先选择要打包的文件！", 
                "提示", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (currentConfig == null || currentConfig.getProjectName() == null) {
            JOptionPane.showMessageDialog(this, 
                "缺少项目配置信息！", 
                "错误", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 确认对话框
        int result = JOptionPane.showConfirmDialog(this,
                String.format("确定要打包 %d 个文件到项目 '%s' 吗？", 
                        selectedFiles.size(), currentConfig.getProjectName()),
                "确认打包",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            packageButton.setEnabled(false);
            updateStatus("正在打包...");
            
            if (packageListener != null) {
                packageListener.onPackageRequested(selectedFiles, currentConfig.getProjectName());
            }
        }
    }
    
    /**
     * 打包完成回调
     */
    public void onPackageCompleted(String outputPath) {
        SwingUtilities.invokeLater(() -> {
            packageButton.setEnabled(true);
            updateStatus("打包完成: " + outputPath);
            
            JOptionPane.showMessageDialog(this,
                    "文件打包完成！\n输出路径: " + outputPath,
                    "打包成功",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    /**
     * 打包错误回调
     */
    public void onPackageError(String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            packageButton.setEnabled(true);
            updateStatus("打包失败: " + errorMessage);
            
            JOptionPane.showMessageDialog(this,
                    "打包失败：\n" + errorMessage,
                    "打包错误",
                    JOptionPane.ERROR_MESSAGE);
        });
    }
    
    /**
     * 现代化文件信息渲染器
     */
    private static class FileInfoCellRenderer extends JPanel implements ListCellRenderer<FileInfo> {
        private JLabel iconLabel;
        private JLabel nameLabel;
        private JLabel sizeLabel;
        private JLabel statusLabel;
        private boolean isItemSelected;
        private boolean isItemChecked;
        private FileInfo fileInfo;
        
        // 颜色定义
        private static final Color SELECTED_BG = Color.WHITE; // 选中时保持白色背景
        private static final Color SELECTED_BORDER = Color.RED; // 红色边框表示选中
        private static final Color NORMAL_BG = Color.WHITE;
        private static final Color NORMAL_FG = new Color(51, 51, 51);
        private static final Color SECONDARY_FG = new Color(108, 117, 125);
        private static final Color NORMAL_BORDER = new Color(222, 226, 230);
        private static final Color HOVER_BG = new Color(248, 249, 250);
        
        public FileInfoCellRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3), // 增加外边距
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15) // 增加内边距
                )
            ));
            
            // 左侧：选择状态图标（隐藏）
            iconLabel = new JLabel();
            iconLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
            iconLabel.setPreferredSize(new Dimension(0, 20)); // 设置为0宽度，实际上隐藏
            iconLabel.setVisible(false);
            add(iconLabel, BorderLayout.WEST);
            
            // 中间：文件信息面板
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);
            
            // 文件名 - 使用更大的字体和更好的行间距
            nameLabel = new JLabel();
            nameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14)); // 增大字体从13到14
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // 文件大小和状态 - 使用垂直布局提供更多空间
            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setOpaque(false);
            detailPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            sizeLabel = new JLabel();
            sizeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12)); // 增大字体从11到12
            sizeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            statusLabel = new JLabel();
            statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12)); // 增大字体从11到12
            statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            detailPanel.add(sizeLabel);
            detailPanel.add(Box.createVerticalStrut(3)); // 增加行间距从2到3
            detailPanel.add(statusLabel);
            
            infoPanel.add(nameLabel);
            infoPanel.add(Box.createVerticalStrut(4)); // 增加文件名和详情的间距从2到4
            infoPanel.add(detailPanel);
            
            add(infoPanel, BorderLayout.CENTER);
            
            setPreferredSize(new Dimension(0, 85)); // 进一步增加高度从68到85，提供更充足的显示空间
        }
        
        @Override
        public Component getListCellRendererComponent(JList<? extends FileInfo> list, FileInfo value,
                int index, boolean isSelected, boolean cellHasFocus) {
            
            this.fileInfo = value;
            this.isItemSelected = isSelected;
            this.isItemChecked = value != null && value.isSelected();
            
            if (value != null) {
                // 隐藏选择状态图标，只用边框表示选中
                iconLabel.setText("");
                iconLabel.setVisible(false);
                
                // 设置文件名
                nameLabel.setText(value.getFormattedFileName());
                
                // 设置文件大小
                sizeLabel.setText(value.getFormattedFileSize());
                
                // 设置状态信息
                String status = "后缀: " + value.getSuffix();
                if (value.getPrefix() != null && !value.getPrefix().isEmpty()) {
                    status = "[" + value.getPrefix() + "] " + status;
                }
                statusLabel.setText(status);
                
                // 设置颜色主题
                updateColors();
                
                // 设置工具提示
                setToolTipText(String.format(
                    "<html><div style='padding: 4px;'>" +
                    "<b>%s</b><br/>" +
                    "大小: %s<br/>" +
                    "前缀: %s<br/>" +
                    "后缀: %d<br/>" +
                    "路径: %s<br/>" +
                    "<i>提示：双击选中/取消选中</i>" +
                    "</div></html>",
                    value.getOriginalFileName(),
                    value.getFormattedFileSize(),
                    value.getPrefix() != null ? value.getPrefix() : "无",
                    value.getSuffix(),
                    value.getFilePath()
                ));
            } else {
                iconLabel.setText("");
                nameLabel.setText("");
                sizeLabel.setText("");
                statusLabel.setText("");
                setToolTipText(null);
            }
            
            return this;
        }
        
        private void updateColors() {
            Color bgColor, fgColor, secondaryColor, borderColor;
            int borderWidth;
            
            if (isItemChecked) {
                // 已选中状态：红色边框（这是最高优先级）
                bgColor = SELECTED_BG;
                fgColor = NORMAL_FG;
                secondaryColor = SECONDARY_FG;
                borderColor = SELECTED_BORDER; // 红色边框
                borderWidth = 2; // 更粗的边框表示选中
            } else if (isItemSelected) {
                // 鼠标悬停状态：浅灰色背景
                bgColor = HOVER_BG;
                fgColor = NORMAL_FG;
                secondaryColor = SECONDARY_FG;
                borderColor = NORMAL_BORDER;
                borderWidth = 1;
            } else {
                // 普通状态
                bgColor = NORMAL_BG;
                fgColor = NORMAL_FG;
                secondaryColor = SECONDARY_FG;
                borderColor = NORMAL_BORDER;
                borderWidth = 1;
            }
            
            setBackground(bgColor);
            nameLabel.setForeground(fgColor);
            sizeLabel.setForeground(secondaryColor);
            statusLabel.setForeground(secondaryColor);
            
            // 更新边框：选中时使用红色粗边框
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3), // 增加外边距
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, borderWidth),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15) // 增加内边距
                )
            ));
            
            // 设置组件透明度
            setOpaque(true);
            
            // 递归设置子组件背景色
            updateChildrenBackground(this, bgColor);
        }
        
        private void updateChildrenBackground(Container container, Color bgColor) {
            for (Component comp : container.getComponents()) {
                if (comp instanceof JPanel) {
                    comp.setBackground(bgColor);
                    if (container instanceof Container) {
                        updateChildrenBackground((Container) comp, bgColor);
                    }
                }
            }
        }
    }
}