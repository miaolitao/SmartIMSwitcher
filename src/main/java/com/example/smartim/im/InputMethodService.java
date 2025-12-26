package com.example.smartim.im;

/**
 * 输入法切换服务接口
 */
public interface InputMethodService {
    /**
     * 切换到中文输入法撒
     */
    void switchToNative();

    /**
     * 切换到英文输入法
     */
    void switchToEnglish();

    /**
     * 根据名称切换输入法
     */
    boolean switchByName(String name);

    /**
     * 当前是否为中文输入法（可选实现）
     */
    boolean isChinese();
}
