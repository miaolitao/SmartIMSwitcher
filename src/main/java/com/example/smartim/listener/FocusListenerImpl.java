package com.example.smartim.listener;

import com.example.smartim.im.InputMethodService;
import com.example.smartim.im.MacInputMethodService;
import com.example.smartim.settings.SmartIMSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.jetbrains.annotations.NotNull;

public class FocusListenerImpl implements ToolWindowManagerListener {
    private final InputMethodService imService = new MacInputMethodService();
    private final SmartIMSettings settings = SmartIMSettings.getInstance();

    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        if (!settings.isEnabled())
            return;

        String activeId = toolWindowManager.getActiveToolWindowId();
        if (activeId == null)
            return;

        // 当切到 Terminal 或 Project View 时，自动切回英文
        if (activeId.equals("Terminal") || activeId.equals("Project")) {
            imService.switchToEnglish();
        }
    }
}
