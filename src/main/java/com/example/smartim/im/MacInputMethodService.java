package com.example.smartim.im;

import com.example.smartim.settings.SmartIMSettings;
import com.intellij.openapi.diagnostic.Logger;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * macOS 实现：解决 Sogou 输入法在 Plist 中由于在 History 而不在 Enabled 导致的无法获取问题
 */
public class MacInputMethodService implements InputMethodService {
    private static final Logger LOG = Logger.getInstance(MacInputMethodService.class);

    @Override
    public void switchToNative() {
        String name = SmartIMSettings.getInstance().chineseIMName;
        LOG.info("[SmartIM] 尝试切换到中文: " + name);
        if (!switchByName(name)) {
            LOG.warn("[SmartIM] 按名称切换失败，尝试执行基础切换脚本兜底");
            executeAppleScript(SmartIMSettings.getInstance().chineseIMScript, true);
        }
    }

    @Override
    public void switchToEnglish() {
        String name = SmartIMSettings.getInstance().englishIMName;
        LOG.info("[SmartIM] 尝试切换到英文: " + name);
        if (!switchByName(name)) {
            LOG.warn("[SmartIM] 按名称切换失败，尝试执行基础切换脚本兜底");
            executeAppleScript(SmartIMSettings.getInstance().englishIMScript, true);
        }
    }

    @Override
    public boolean isChinese() {
        return false;
    }

    /**
     * 通过扫描 HIToolbox 的多个关键键值获取输入法列表
     */
    public List<String> getInstalledInputMethods() {
        LOG.info("[SmartIM] 开始刷新输入法列表 (全量扫描模式)...");
        List<String> names = new ArrayList<>();

        // 扫描多个来源：Enabled (启用的), History (最近使用的), Selected (当前选中的)
        String[] keys = { "AppleEnabledInputSources", "AppleInputSourceHistory", "AppleSelectedInputSources" };

        for (String key : keys) {
            try {
                Process process = new ProcessBuilder("defaults", "read", "com.apple.HIToolbox", key).start();
                String output;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    output = reader.lines().collect(Collectors.joining("\n"));
                }

                if (output.isEmpty())
                    continue;
                LOG.info("[SmartIM] 正在扫描 Plist 键: " + key);

                // 提取逻辑
                Pattern p1 = Pattern.compile("\"KeyboardLayout Name\"\\s*=\\s*\"?([^\";]+)\"?;");
                Matcher m1 = p1.matcher(output);
                while (m1.find()) {
                    addUnique(names, m1.group(1).trim());
                }

                Pattern p2 = Pattern.compile("\"Input Mode\"\\s*=\\s*\"?([^\";]+)\"?;");
                Matcher m2 = p2.matcher(output);
                while (m2.find()) {
                    String id = m2.group(1).trim();
                    if (id.contains("sogou")) {
                        addUnique(names, "搜狗拼音");
                        LOG.info("[SmartIM] 映射 Sogou: " + id + " -> 搜狗拼音");
                    } else if (id.contains("SCIM.ITABC")) {
                        addUnique(names, "ABC");
                    } else if (id.contains("SCIM.Pinyin")) {
                        addUnique(names, "简体拼音");
                    } else {
                        String[] parts = id.split("\\.");
                        addUnique(names, parts[parts.length - 1]);
                    }
                }
            } catch (Exception e) {
                LOG.error("[SmartIM] 扫描 " + key + " 失败", e);
            }
        }

        // 兜底：如果还是太少，增加核心常项
        if (!names.contains("ABC"))
            names.add(0, "ABC");
        if (!names.contains("搜狗拼音"))
            names.add("搜狗拼音");

        LOG.info("[SmartIM] 扫描完成: " + names);
        return names;
    }

    private void addUnique(List<String> list, String item) {
        if (!list.contains(item))
            list.add(item);
    }

    private String cachedIM = null;
    private File externalToolFile = null;

    public MacInputMethodService() {
        ensureExternalToolExists();
    }

    private void ensureExternalToolExists() {
        try {
            // 在临时目录创建文件
            externalToolFile = new File(System.getProperty("java.io.tmpdir"), "smart_im_switch");

            // 每次启动都重新释放，确保工具是最新的
            try (InputStream is = getClass().getResourceAsStream("/im-switch")) {
                if (is == null) {
                    LOG.error("[SmartIM] 严重错误：未在资源中找到 im-switch 工具！");
                    return;
                }
                try (FileOutputStream fos = new FileOutputStream(externalToolFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }
            }

            // 赋予执行权限
            if (!externalToolFile.setExecutable(true)) {
                LOG.warn("[SmartIM] 无法设置 im-switch 为可执行，尝试 chmod 命令");
                new ProcessBuilder("chmod", "+x", externalToolFile.getAbsolutePath()).start().waitFor();
            }

            LOG.info("[SmartIM] im-switch 工具已就绪: " + externalToolFile.getAbsolutePath());
        } catch (Exception e) {
            LOG.error("[SmartIM] 初始化 im-switch 工具失败", e);
        }
    }

    @Override
    public boolean switchByName(String name) {
        if (name == null || name.isEmpty() || name.equals("保持现状"))
            return true;

        // 缓存机制：如果目标输入法与上次成功切换的一致，则跳过执行
        if (name.equals(cachedIM)) {
            LOG.info("[SmartIM] 目标输入法 [" + name + "] 与当前缓存一致，跳过切换");
            return true;
        }

        if (externalToolFile == null || !externalToolFile.exists()) {
            LOG.error("[SmartIM] im-switch 工具未就绪，尝试重新释放");
            ensureExternalToolExists();
            if (externalToolFile == null || !externalToolFile.exists()) {
                return false;
            }
        }

        // 使用基于 Carbon API 的 im-switch 命令行工具
        try {
            ProcessBuilder pb = new ProcessBuilder(externalToolFile.getAbsolutePath(), name);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining("\n")).trim();
            int code = process.waitFor();

            LOG.info("[SmartIM] im-switch (" + name + ") result: " + output + " (exit: " + code + ")");

            boolean success = "true".equalsIgnoreCase(output);
            if (success) {
                this.cachedIM = name;
            }
            return success;
        } catch (Exception e) {
            LOG.error("[SmartIM] im-switch 调用失败: " + e.getMessage());
            return false;
        }
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

    private String executeAppleScriptWithResult(String script) {
        try {
            Process process = new ProcessBuilder("osascript", "-e", script).start();
            String out = new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                    .collect(Collectors.joining("\n"));
            process.waitFor();
            return out.trim();
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        }
    }
}
