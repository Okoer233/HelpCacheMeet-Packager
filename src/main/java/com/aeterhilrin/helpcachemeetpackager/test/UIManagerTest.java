package com.aeterhilrin.helpcachemeetpackager.test;

import javax.swing.*;

/**
 * UIManager API 测试类
 */
public class UIManagerTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("测试 UIManager API...");
            
            // 测试修复后的API调用
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.out.println("✅ UIManager.getSystemLookAndFeelClassName() 调用成功");
            
            // 设置默认字体以支持中文
            java.awt.Font font = new java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, 12);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            int fontCount = 0;
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, new javax.swing.plaf.FontUIResource(font));
                    fontCount++;
                }
            }
            System.out.println("✅ 字体设置完成，设置了 " + fontCount + " 个字体属性");
            
            // 设置一些UI属性
            UIManager.put("OptionPane.yesButtonText", "是");
            UIManager.put("OptionPane.noButtonText", "否");
            UIManager.put("OptionPane.cancelButtonText", "取消");
            UIManager.put("OptionPane.okButtonText", "确定");
            System.out.println("✅ UI属性设置完成");
            
            System.out.println("所有UIManager API测试通过！");
            
        } catch (Exception e) {
            System.err.println("❌ UIManager测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}