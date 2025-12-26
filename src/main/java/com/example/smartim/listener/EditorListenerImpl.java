package com.example.smartim.listener;

import com.example.smartim.core.ContextDetector;
import com.example.smartim.core.ContextDetector.ContextType;
import com.example.smartim.im.InputMethodService;
import com.example.smartim.im.MacInputMethodService;
import com.example.smartim.settings.SmartIMSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.editor.CaretVisualAttributes;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

/**
 * 编辑器监听器：实现核心切换逻辑
 */
public class EditorListenerImpl implements EditorFactoryListener {
    private static final Logger LOG = Logger.getInstance(EditorListenerImpl.class);

    private final InputMethodService imService = new MacInputMethodService();
    private final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, null);

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        LOG.info("[SmartIM] 编辑器已创建，注册光标监听器");
        editor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent e) {
                SmartIMSettings settings = SmartIMSettings.getInstance();
                if (!settings.isEnabled())
                    return;

                alarm.cancelAllRequests();
                alarm.addRequest(() -> {
                    if (editor.isDisposed())
                        return;
                    ApplicationManager.getApplication().runReadAction(() -> {
                        updateInputMethod(editor, settings);
                    });
                }, settings.getDebounceMs());
            }
        });
    }

    private void updateInputMethod(Editor editor, SmartIMSettings settings) {
        ContextType context = ContextDetector.getContext(editor);
        SmartIMSettings.ContextSettings langSettings = getSettingsForEditor(editor, settings);

        LOG.info("[SmartIM] 触发上下文检测 -> 场景: " + context + ", 手动关键词匹配检查中...");

        String targetIM = "保持现状";

        // 处理自定义关键词 (在 CODE 场景下生效)
        if (context == ContextType.CODE && !langSettings.customKeywords.isEmpty()) {
            if (checkCustomKeywords(editor, langSettings.customKeywords)) {
                LOG.info("[SmartIM] 命中了自定义场景匹配关键词 -> 切换至中文");
                targetIM = "默认中文";
            }
        }

        if (targetIM.equals("保持现状")) {
            switch (context) {
                case STRING_LITERAL:
                    targetIM = langSettings.stringLiteral;
                    break;
                case CONSTANT_LITERAL:
                    targetIM = langSettings.constantLiteral;
                    break;
                case SINGLE_LINE_COMMENT:
                    targetIM = langSettings.singleComment;
                    break;
                case MULTI_LINE_COMMENT:
                    targetIM = langSettings.multiComment;
                    break;
                case DOC_COMMENT:
                    targetIM = langSettings.docComment;
                    break;
                case GIT_COMMIT:
                case CHINESE_KEYWORD:
                    targetIM = settings.chineseIMName;
                    break;
                case CODE:
                default:
                    targetIM = settings.englishIMName;
                    break;
            }
        }

        LOG.info("[SmartIM] 最终切换决策: [" + targetIM + "] (基于场景配置)");

        imService.switchByName(targetIM);
        // 这里判断一下颜色：如果是基础设置里的中文，或者名称里带中文特征，变颜色
        boolean shouldBeChineseColor = targetIM.equals(settings.chineseIMName)
                || targetIM.contains("拼音")
                || targetIM.contains("输入法")
                || targetIM.contains("Pinyin");
        updateCaretColor(editor, settings, shouldBeChineseColor);
    }

    private void updateCaretColor(Editor editor, SmartIMSettings settings, boolean isChinese) {
        String hex = isChinese ? settings.chineseCursorColor : settings.englishCursorColor;
        LOG.debug("[SmartIM] 尝试更新光标颜色为: " + hex);
        try {
            java.awt.Color color = java.awt.Color.decode(hex);
            editor.getCaretModel().getPrimaryCaret().setVisualAttributes(
                    new CaretVisualAttributes(color, CaretVisualAttributes.Weight.NORMAL));
        } catch (Exception e) {
            LOG.warn("[SmartIM] 光标颜色解析失败: " + hex);
        }
    }

    private SmartIMSettings.ContextSettings getSettingsForEditor(Editor editor, SmartIMSettings settings) {
        Project project = editor.getProject();
        if (project == null)
            return settings.generalSettings;

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null)
            return settings.generalSettings;

        String langId = psiFile.getLanguage().getID();
        LOG.debug("[SmartIM] 当前文件语言 ID: " + langId);

        String langIdLower = langId.toLowerCase();
        if (langIdLower.equals("java"))
            return settings.javaSettings;
        if (langIdLower.equals("kotlin"))
            return settings.kotlinSettings;
        if (langIdLower.contains("python"))
            return settings.pythonSettings;

        return settings.generalSettings;
    }

    private boolean checkCustomKeywords(Editor editor, String keywords) {
        // 获取当前行文本
        int offset = editor.getCaretModel().getOffset();
        int lineNum = editor.getDocument().getLineNumber(offset);
        int lineStart = editor.getDocument().getLineStartOffset(lineNum);
        try {
            String lineText = editor.getDocument().getText(new com.intellij.openapi.util.TextRange(lineStart, offset));
            for (String keyword : keywords.split(";")) {
                String trimmed = keyword.trim();
                if (!trimmed.isEmpty() && lineText.contains(trimmed)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
