package com.example.smartim.core;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralValue;

public class ContextDetector {

    public enum ContextType {
        CODE,
        CHINESE_KEYWORD, // 自定义中文关键词
        STRING_LITERAL,
        CONSTANT_LITERAL,
        SINGLE_LINE_COMMENT,
        MULTI_LINE_COMMENT,
        DOC_COMMENT,
        GIT_COMMIT
    }

    public static ContextType getContext(Editor editor) {
        Project project = editor.getProject();
        if (project == null)
            return ContextType.CODE;

        // 识别 Git 提交窗口
        String name = editor.getClass().getName();
        if (name.contains("Commit") || name.contains("Checkin")) {
            return ContextType.GIT_COMMIT;
        }

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null)
            return ContextType.CODE;

        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);

        if (element == null && offset > 0) {
            element = psiFile.findElementAt(offset - 1);
        }

        if (element != null) {
            // 注释场景
            PsiComment comment = getParentComment(element);
            if (comment != null) {
                String commentType = comment.getTokenType().toString();
                if (commentType.contains("DOC"))
                    return ContextType.DOC_COMMENT;
                if (commentType.contains("BLOCK") || commentType.contains("MULTI"))
                    return ContextType.MULTI_LINE_COMMENT;
                return ContextType.SINGLE_LINE_COMMENT;
            }

            // 字符串场景
            if (isInsideString(element)) {
                // 判断是否是常量 (简单判断)
                if (isConstant(element))
                    return ContextType.CONSTANT_LITERAL;
                return ContextType.STRING_LITERAL;
            }
        }

        return ContextType.CODE;
    }

    private static PsiComment getParentComment(PsiElement element) {
        PsiElement current = element;
        while (current != null && !(current instanceof PsiFile)) {
            if (current instanceof PsiComment)
                return (PsiComment) current;
            current = current.getParent();
        }
        return null;
    }

    private static boolean isInsideString(PsiElement element) {
        PsiElement current = element;
        while (current != null && !(current instanceof PsiFile)) {
            if (current instanceof PsiLiteralValue || current.getClass().getSimpleName().contains("StringLiteral")) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static boolean isConstant(PsiElement element) {
        String text = element.getText();
        return text.toUpperCase().equals(text) && text.length() > 2 && text.contains("_");
    }
}
