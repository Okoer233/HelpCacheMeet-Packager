package com.aeterhilrin.helpcachemeetpackager.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 协议确认对话框
 * 在应用程序启动时显示软件使用协议
 * 
 * @author AeterHilrin
 */
public class LicenseDialog extends JDialog {
    private boolean accepted = false;
    
    private static final String LICENSE_TEXT = 
        "本软件基于Mit协议开源(过一会儿再发，git玩的不是很明白)\n\n" +
        "本软件仅做学习交流之用，任何因使用者商业化行为或其他行为导致的纠纷与开发者无关!\n\n" +
        "注意！你可以随意的分发，反编译，重构或将代码应用于自己的项目中，但你不能宣称该软件是你本人开发！\n\n" +
        "如果你修改了源码或者在自己的项目中使用了本项目源码，\n" +
        "请注意务必要在涉及到本项目代码处著名来源或者在项目概览中提及本项目(这点其实我无所谓，因为是Mit协议)\n" +
        "提及本项目以及其github地址\n\n" +
        "如果遇到不妥可以加入本人常驻的开发群:893398880或者490989498\n\n" +
        "本人QQ号：哼哼哼啊啊啊啊啊啊啊啊啊啊啊\n\n" +
        "——By AeterHilrin";
    
    public LicenseDialog(Frame parent) {
        super(parent, "软件使用协议", true);
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupDialog();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 设置对话框属性
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 标题
        JLabel titleLabel = new JLabel("软件使用协议", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(titleLabel, BorderLayout.NORTH);
        
        // 协议内容
        JTextArea licenseTextArea = new JTextArea(LICENSE_TEXT);
        licenseTextArea.setEditable(false);
        licenseTextArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        licenseTextArea.setLineWrap(true);
        licenseTextArea.setWrapStyleWord(true);
        licenseTextArea.setBackground(getBackground());
        licenseTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(licenseTextArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        add(scrollPane, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton acceptButton = new JButton("同意");
        JButton declineButton = new JButton("不同意");
        
        // 设置按钮样式
        acceptButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        declineButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        acceptButton.setPreferredSize(new Dimension(80, 30));
        declineButton.setPreferredSize(new Dimension(80, 30));
        
        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // 设置事件处理
        acceptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accepted = true;
                dispose();
            }
        });
        
        declineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accepted = false;
                dispose();
            }
        });
    }
    
    /**
     * 设置事件处理
     */
    private void setupEventHandlers() {
        // 窗口关闭事件
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                accepted = false;
                dispose();
            }
        });
        
        // ESC键关闭
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke("ESCAPE");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accepted = false;
                dispose();
            }
        });
    }
    
    /**
     * 设置对话框属性
     */
    private void setupDialog() {
        pack();
        setLocationRelativeTo(getParent());
    }
    
    /**
     * 显示协议对话框并返回用户选择
     * @return 用户是否同意协议
     */
    public boolean showLicenseDialog() {
        setVisible(true);
        return accepted;
    }
    
    /**
     * 静态方法：显示协议对话框
     * @param parent 父窗口
     * @return 用户是否同意协议
     */
    public static boolean showLicense(Frame parent) {
        LicenseDialog dialog = new LicenseDialog(parent);
        return dialog.showLicenseDialog();
    }
    
    /**
     * 检查用户是否接受协议
     * @return 是否接受协议
     */
    public boolean isAccepted() {
        return accepted;
    }
}