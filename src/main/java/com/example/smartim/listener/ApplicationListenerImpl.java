package com.example.smartim.listener;

import com.example.smartim.im.InputMethodService;
import com.example.smartim.im.MacInputMethodService;
import com.example.smartim.settings.SmartIMSettings;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.wm.IdeFrame;
import org.jetbrains.annotations.NotNull;

/**
 * 监听 IDE 激活/失去焦点事件
 */
public class ApplicationListenerImpl implements ApplicationActivationListener {
    private static final Logger LOG = Logger.getInstance(ApplicationListenerImpl.class);
    private final InputMethodService imService = new MacInputMethodService();

    @Override
    public void applicationDeactivated(@NotNull IdeFrame ideFrame) {
        SmartIMSettings settings = SmartIMSettings.getInstance();
        if (!settings.isEnabled())
            return;

        String mode = settings.leaveIDEMode;
        LOG.info("[SmartIM] 监测到 IDE 失去焦点, 离开模式配置为: " + mode);

        // 处理可能的遗留配置或直接切换
        if ("中文".equals(mode) || "默认中文".equals(mode)) {
            imService.switchToNative();
        } else if ("英文".equals(mode) || "默认英文".equals(mode)) {
            imService.switchToEnglish();
        } else if (mode != null && !mode.isEmpty() && !"保持现状".equals(mode)) {
            imService.switchByName(mode);
        }
    }

    @Override
    public void applicationActivated(@NotNull IdeFrame ideFrame) {
        LOG.debug("[SmartIM] 监测到 IDE 获得焦点");
    }
}
