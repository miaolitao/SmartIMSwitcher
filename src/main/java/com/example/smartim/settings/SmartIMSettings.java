package com.example.smartim.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 插件设置持久化存储
 */
@Service
@State(name = "SmartIMSettings", storages = @Storage("SmartIMSwitcher.xml"))
public final class SmartIMSettings implements PersistentStateComponent<SmartIMSettings> {

    public boolean enabled = true;
    public int debounceMs = 300;

    // 基础配置
    public String chineseIMScript = "tell application \"System Events\" to key code 102";
    public String englishIMScript = "tell application \"System Events\" to key code 104";
    public String chineseIMName = "搜狗拼音";
    public String englishIMName = "ABC";
    public String leaveIDEMode = "英文";

    // 光标颜色 (Hex)
    public String englishCursorColor = "#808080";
    public String chineseCursorColor = "#FF0000";
    public String capsLockCursorColor = "#FFD700";

    // 场景配置类
    public static class ContextSettings {
        // 存储具体的输入法名称，或者是 "默认中文", "默认英文", "保持现状"
        public String stringLiteral = "默认英文";
        public String constantLiteral = "默认英文";
        public String singleComment = "默认中文";
        public String multiComment = "默认中文";
        public String docComment = "默认中文";
        public String customKeywords = "";
    }

    public ContextSettings generalSettings = new ContextSettings();
    public ContextSettings javaSettings = new ContextSettings();
    public ContextSettings kotlinSettings = new ContextSettings();
    public ContextSettings pythonSettings = new ContextSettings();

    @Nullable
    @Override
    public SmartIMSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SmartIMSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getDebounceMs() {
        return debounceMs;
    }

    public static SmartIMSettings getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(SmartIMSettings.class);
    }
}
