package com.example.smartim.im;

import com.example.smartim.im.jna.CarbonWrapper;
import com.example.smartim.settings.SmartIMSettings;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jna.Pointer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * macOS 实现：使用 JNA 调用 Carbon TIS API 直接切换输入法，无需外部工具
 */
public class MacInputMethodService implements InputMethodService {
    private static final Logger LOG = Logger.getInstance(MacInputMethodService.class);
    private final CarbonWrapper carbon = CarbonWrapper.INSTANCE;

    private String cachedIM = null;

    public MacInputMethodService() {
        // No initialization needed for JNA wrapper
    }

    @Override
    public void switchToNative() {
        String name = SmartIMSettings.getInstance().chineseIMName;
        LOG.info("[SmartIM] 尝试切换到中文: " + name);
        if (!switchByName(name)) {
            // Fallback to script only if defined and native failed (optional, but keeping
            // logical flow)
            LOG.warn("[SmartIM] Native切换失败，尝试执行基础切换脚本兜底");
            executeAppleScript(SmartIMSettings.getInstance().chineseIMScript, true);
        }
    }

    @Override
    public void switchToEnglish() {
        String name = SmartIMSettings.getInstance().englishIMName;
        LOG.info("[SmartIM] 尝试切换到英文: " + name);
        if (!switchByName(name)) {
            LOG.warn("[SmartIM] Native切换失败，尝试执行基础切换脚本兜底");
            executeAppleScript(SmartIMSettings.getInstance().englishIMScript, true);
        }
    }

    @Override
    public boolean isChinese() {
        return false;
    }

    public List<String> getInstalledInputMethods() {
        LOG.info("[SmartIM] 开始刷新输入法列表 (Native API)...");
        List<String> names = new ArrayList<>();

        Pointer sourceList = carbon.TISCreateInputSourceList(null, true);
        if (sourceList == null) {
            LOG.error("[SmartIM] 无法获取输入法列表 (TISCreateInputSourceList returned null)");
            return names;
        }

        try {
            long count = carbon.CFArrayGetCount(sourceList);
            for (long i = 0; i < count; i++) {
                Pointer source = carbon.CFArrayGetValueAtIndex(sourceList, i);
                if (source == null)
                    continue;

                // Check category
                Pointer categoryPtr = carbon.TISGetInputSourceProperty(source,
                        CarbonWrapper.Keys.kTISPropertyInputSourceCategory);
                String category = carbon.toJavaString(categoryPtr);
                if (!"TISCategoryKeyboardInputSource".equals(category)) {
                    continue; // Skip non-keyboard sources
                }

                // Get ID
                Pointer idPtr = carbon.TISGetInputSourceProperty(source, CarbonWrapper.Keys.kTISPropertyInputSourceID);
                String id = carbon.toJavaString(idPtr);

                if (id != null && !id.isEmpty()) {
                    names.add(id);
                }
            }
        } finally {
            // CFRelease isn't stricly required for short lived plugin actions but good
            // practice if wrapper supported it
        }

        LOG.info("[SmartIM] 扫描完成: " + names);
        return names;
    }

    @Override
    public boolean switchByName(String name) {
        if (name == null || name.isEmpty() || name.equals("保持现状"))
            return true;

        if (name.equals(cachedIM)) {
            return true;
        }

        Pointer sourceList = carbon.TISCreateInputSourceList(null, true);
        if (sourceList == null)
            return false;

        boolean found = false;
        try {
            long count = carbon.CFArrayGetCount(sourceList);
            for (long i = 0; i < count; i++) {
                Pointer source = carbon.CFArrayGetValueAtIndex(sourceList, i);
                if (source == null)
                    continue;

                Pointer idPtr = carbon.TISGetInputSourceProperty(source, CarbonWrapper.Keys.kTISPropertyInputSourceID);
                String id = carbon.toJavaString(idPtr);

                if (name.equals(id)) {
                    int result = carbon.TISSelectInputSource(source);
                    if (result == 0) { // noErr
                        found = true;
                        cachedIM = name;
                        LOG.info("[SmartIM] 成功切换到: " + name);
                    } else {
                        LOG.error("[SmartIM] TISSelectInputSource 失败, 错误码: " + result);
                    }
                    break;
                }
            }
        } finally {
            // CFRelease(sourceList);
        }

        return found;
    }

    private void executeAppleScript(String script, boolean logError) {
        try {
            Process process = new ProcessBuilder("osascript", "-e", script).start();
            int code = process.waitFor();
            if (code != 0 && logError) {
                String err = new BufferedReader(new InputStreamReader(process.getErrorStream())).lines()
                        .collect(Collectors.joining("\n"));
                LOG.error("[SmartIM] 兜底脚本失败 (" + code + "): " + err);
            }
        } catch (Exception e) {
            if (logError)
                LOG.error("[SmartIM] 脚本致命异常", e);
        }
    }
}
