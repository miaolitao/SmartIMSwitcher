package com.example.smartim.settings;

import com.example.smartim.im.MacInputMethodService;
import com.example.smartim.settings.SmartIMSettings;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件设置界面
 */
public class SmartIMConfigurable implements Configurable {

    private JPanel mainPanel;
    private JCheckBox enabledCheckBox;
    private JTextField debounceField;
    private JComboBox<String> leaveIDEModeCombo;

    private JComboBox<String> chineseIMCombo;
    private JComboBox<String> englishIMCombo;
    private JTextField englishColorField;
    private JTextField chineseColorField;
    private JTextField capsColorField;

    // 场景配置组件辅助类
    private static class LangUI {
        JComboBox<String> stringIM = new JComboBox<>();
        JComboBox<String> constantIM = new JComboBox<>();
        JComboBox<String> singleCommentIM = new JComboBox<>();
        JComboBox<String> multiCommentIM = new JComboBox<>();
        JComboBox<String> docCommentIM = new JComboBox<>();
        JTextArea customKeywords = new JTextArea(4, 30);

        void load(SmartIMSettings.ContextSettings s) {
            stringIM.setSelectedItem(s.stringLiteral);
            constantIM.setSelectedItem(s.constantLiteral);
            singleCommentIM.setSelectedItem(s.singleComment);
            multiCommentIM.setSelectedItem(s.multiComment);
            docCommentIM.setSelectedItem(s.docComment);
            customKeywords.setText(s.customKeywords);
        }

        boolean isModified(SmartIMSettings.ContextSettings s) {
            return !String.valueOf(stringIM.getSelectedItem()).equals(s.stringLiteral) ||
                    !String.valueOf(constantIM.getSelectedItem()).equals(s.constantLiteral) ||
                    !String.valueOf(singleCommentIM.getSelectedItem()).equals(s.singleComment) ||
                    !String.valueOf(multiCommentIM.getSelectedItem()).equals(s.multiComment) ||
                    !String.valueOf(docCommentIM.getSelectedItem()).equals(s.docComment) ||
                    !customKeywords.getText().equals(s.customKeywords);
        }

        void apply(SmartIMSettings.ContextSettings s) {
            s.stringLiteral = (String) stringIM.getSelectedItem();
            s.constantLiteral = (String) constantIM.getSelectedItem();
            s.singleComment = (String) singleCommentIM.getSelectedItem();
            s.multiComment = (String) multiCommentIM.getSelectedItem();
            s.docComment = (String) docCommentIM.getSelectedItem();
            s.customKeywords = customKeywords.getText();
        }

        void updateOptions(List<String> systemIMs) {
            updateCombo(stringIM, systemIMs);
            updateCombo(constantIM, systemIMs);
            updateCombo(singleCommentIM, systemIMs);
            updateCombo(multiCommentIM, systemIMs);
            updateCombo(docCommentIM, systemIMs);
        }

        void syncChinese(String name) {
            singleCommentIM.setSelectedItem(name);
            multiCommentIM.setSelectedItem(name);
            docCommentIM.setSelectedItem(name);
        }

        void syncEnglish(String name) {
            stringIM.setSelectedItem(name);
            constantIM.setSelectedItem(name);
        }

        private void updateCombo(JComboBox<String> combo, List<String> systemIMs) {
            Object selected = combo.getSelectedItem();
            combo.removeAllItems();
            for (String im : systemIMs) {
                combo.addItem(im);
            }
            if (selected != null) {
                combo.setSelectedItem(selected);
            }
        }
    }

    private final LangUI generalUI = new LangUI();
    private final LangUI javaUI = new LangUI();
    private final LangUI kotlinUI = new LangUI();
    private final LangUI pythonUI = new LangUI();

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Smart IM Switcher";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("基础配置", createMainPanel());
        tabbedPane.addTab("通用场景配置", createLangPanel(generalUI));
        tabbedPane.addTab("Java场景配置", createLangPanel(javaUI));
        tabbedPane.addTab("Kotlin场景配置", createLangPanel(kotlinUI));
        tabbedPane.addTab("Python/GO/JS等场景配置", createLangPanel(pythonUI));

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createMainPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        enabledCheckBox = new JCheckBox("启用智能输入法切换");
        debounceField = new JTextField(10);
        leaveIDEModeCombo = new JComboBox<>();

        chineseIMCombo = new JComboBox<>();
        chineseIMCombo.setEditable(true);
        englishIMCombo = new JComboBox<>();
        englishIMCombo.setEditable(true);

        JButton refreshBtn = new JButton("刷新系统输入法列表");
        refreshBtn.addActionListener(e -> refreshIMNames());

        englishColorField = new JTextField(10);
        chineseColorField = new JTextField(10);
        capsColorField = new JTextField(10);

        addLabeledComponent(p, "核心开关:", enabledCheckBox);
        addLabeledComponent(p, "防抖延迟 (ms):", debounceField);

        JPanel imPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        imPanel.add(refreshBtn);
        addLabeledComponent(p, "同步列表:", imPanel);

        addLabeledComponent(p, "选择系统的中文输入法:", chineseIMCombo);
        addLabeledComponent(p, "选择系统的英文输入法:", englishIMCombo);

        // 添加自动同步监听
        chineseIMCombo.addActionListener(e -> {
            String name = (String) chineseIMCombo.getSelectedItem();
            if (name != null) {
                generalUI.syncChinese(name);
                javaUI.syncChinese(name);
                kotlinUI.syncChinese(name);
                pythonUI.syncChinese(name);
            }
        });
        englishIMCombo.addActionListener(e -> {
            String name = (String) englishIMCombo.getSelectedItem();
            if (name != null) {
                generalUI.syncEnglish(name);
                javaUI.syncEnglish(name);
                kotlinUI.syncEnglish(name);
                pythonUI.syncEnglish(name);
            }
        });
        addLabeledComponent(p, "离开IDE切换输入法:", leaveIDEModeCombo);
        addLabeledComponent(p, "英文状态时光标颜色 (Hex):", englishColorField);
        addLabeledComponent(p, "中文状态时光标颜色 (Hex):", chineseColorField);
        addLabeledComponent(p, "大写锁定时光标颜色 (Hex):", capsColorField);

        // 初始化时也刷新一下
        refreshIMNames();

        return p;
    }

    private JPanel createLangPanel(LangUI ui) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;

        int row = 0;
        addRow(p, "字符串字面量:", ui.stringIM, c, row++);
        addRow(p, "不可变常量:", ui.constantIM, c, row++);
        addRow(p, "单行注释:", ui.singleCommentIM, c, row++);
        addRow(p, "多行注释:", ui.multiCommentIM, c, row++);
        addRow(p, "文档注释:", ui.docCommentIM, c, row++);

        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        p.add(new JLabel("自定义字符串内容检测（匹配则切中文，分号分隔）:"), c);
        c.gridy = row++;
        p.add(new JScrollPane(ui.customKeywords), c);

        return p;
    }

    private void addLabeledComponent(JPanel parent, String label, JComponent comp) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel(label));
        row.add(comp);
        parent.add(row);
    }

    private void addRow(JPanel p, String label, JComponent comp, GridBagConstraints c, int row) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        p.add(comp, c);
    }

    @Override
    public boolean isModified() {
        SmartIMSettings s = SmartIMSettings.getInstance();
        return enabledCheckBox.isSelected() != s.enabled ||
                !debounceField.getText().equals(String.valueOf(s.debounceMs)) ||
                !String.valueOf(leaveIDEModeCombo.getSelectedItem()).equals(s.leaveIDEMode) ||
                !String.valueOf(chineseIMCombo.getSelectedItem()).equals(s.chineseIMName) ||
                !String.valueOf(englishIMCombo.getSelectedItem()).equals(s.englishIMName) ||
                !englishColorField.getText().equals(s.englishCursorColor) ||
                !chineseColorField.getText().equals(s.chineseCursorColor) ||
                !capsColorField.getText().equals(s.capsLockCursorColor) ||
                generalUI.isModified(s.generalSettings) ||
                javaUI.isModified(s.javaSettings) ||
                kotlinUI.isModified(s.kotlinSettings) ||
                pythonUI.isModified(s.pythonSettings);
    }

    @Override
    public void apply() {
        SmartIMSettings s = SmartIMSettings.getInstance();
        s.enabled = enabledCheckBox.isSelected();
        try {
            s.debounceMs = Integer.parseInt(debounceField.getText());
        } catch (NumberFormatException ignored) {
        }
        s.leaveIDEMode = (String) leaveIDEModeCombo.getSelectedItem();
        s.chineseIMName = (String) chineseIMCombo.getSelectedItem();
        s.englishIMName = (String) englishIMCombo.getSelectedItem();
        s.englishCursorColor = englishColorField.getText();
        s.chineseCursorColor = chineseColorField.getText();
        s.capsLockCursorColor = capsColorField.getText();

        generalUI.apply(s.generalSettings);
        javaUI.apply(s.javaSettings);
        kotlinUI.apply(s.kotlinSettings);
        pythonUI.apply(s.pythonSettings);
    }

    @Override
    public void reset() {
        SmartIMSettings s = SmartIMSettings.getInstance();
        enabledCheckBox.setSelected(s.enabled);
        debounceField.setText(String.valueOf(s.debounceMs));
        leaveIDEModeCombo.setSelectedItem(s.leaveIDEMode);
        chineseIMCombo.setSelectedItem(s.chineseIMName);
        englishIMCombo.setSelectedItem(s.englishIMName);
        englishColorField.setText(s.englishCursorColor);
        chineseColorField.setText(s.chineseCursorColor);
        capsColorField.setText(s.capsLockCursorColor);

        generalUI.load(s.generalSettings);
        javaUI.load(s.javaSettings);
        kotlinUI.load(s.kotlinSettings);
        pythonUI.load(s.pythonSettings);
    }

    private void refreshIMNames() {
        Logger LOG = Logger.getInstance(SmartIMConfigurable.class);
        LOG.info("[SmartIM] 正在调用 MacInputMethodService.getInstalledInputMethods()...");

        MacInputMethodService service = new MacInputMethodService();
        List<String> names = service.getInstalledInputMethods();

        LOG.info("[SmartIM] 最终确定列表项总数: " + names.size());

        Object chineseSel = chineseIMCombo.getSelectedItem();
        Object englishSel = englishIMCombo.getSelectedItem();
        Object leaveSel = leaveIDEModeCombo.getSelectedItem();

        chineseIMCombo.removeAllItems();
        englishIMCombo.removeAllItems();
        leaveIDEModeCombo.removeAllItems();

        // 基础选择只包含原始列表
        for (String name : names) {
            chineseIMCombo.addItem(name);
            englishIMCombo.addItem(name);
            leaveIDEModeCombo.addItem(name);
        }

        // 离开模式不再增加便捷选项，仅保留系统列表

        // 同时更新所有场景页签的下拉框
        generalUI.updateOptions(names);
        javaUI.updateOptions(names);
        kotlinUI.updateOptions(names);
        pythonUI.updateOptions(names);

        if (chineseSel != null)
            chineseIMCombo.setSelectedItem(chineseSel);
        if (englishSel != null)
            englishIMCombo.setSelectedItem(englishSel);
        if (leaveSel != null)
            leaveIDEModeCombo.setSelectedItem(leaveSel);

        try {
            if (names.size() > 0) {
                String msg = "成功同步 " + names.size() + " 个输入法: " + String.join(", ", names);
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("SmartIM Notifications")
                        .createNotification("SmartIM", msg, NotificationType.INFORMATION)
                        .notify(null);
                // 增加强力弹窗，确保用户感知
                Messages.showInfoMessage(msg, "SmartIM 同步结果");
            } else {
                NotificationGroupManager.getInstance()
                        .getNotificationGroup("SmartIM Notifications")
                        .createNotification("SmartIM", "未发现可用输入法，请检查系统设置。", NotificationType.WARNING)
                        .notify(null);
                Messages.showWarningDialog("未能获取到任何输入法项，请检查 macOS 系统设置及辅助功能权限。", "SmartIM 同步警告");
            }
        } catch (Exception e) {
            // 兜底：如果 IDE 还没准备好通知系统
            System.out.println("[SmartIM] 通知失败: " + e.getMessage());
        }
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}
