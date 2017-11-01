package com.andrognito.patternlockview.listener;
import com.andrognito.patternlockview.PatternLockView;

import java.util.List;

/**
 * 手势回调接口
 */
public interface PatternLockViewListener {

    /**
     * 滑动开始
     */
    void onStarted();

    /**
     * 滑动过程中
     */
    void onProgress(List<PatternLockView.Dot> progressPattern);

    /**
     * 滑动完成
     */
    void onComplete(List<PatternLockView.Dot> pattern);

    /**
     * 滑动取消
     */
    void onCleared();
}