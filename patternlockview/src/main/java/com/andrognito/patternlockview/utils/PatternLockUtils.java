/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrognito.patternlockview.utils;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;

import com.andrognito.patternlockview.PatternLockView;

import java.util.List;

/**
 * 手势工具类
 */
public class PatternLockUtils {


    /**
     * 转换成String
     */
    public static String patternToString(PatternLockView patternLockView,
                                         List<PatternLockView.Dot> pattern) {
        if (pattern == null) {
            return "";
        }
        int patternSize = pattern.size();
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < patternSize; i++) {
            PatternLockView.Dot dot = pattern.get(i);
            stringBuilder.append((dot.getRow() * patternLockView.getDotCount() + dot.getColumn()));
        }
        return stringBuilder.toString();
    }

    /**
     * 震动
     */
    public static void shock(TextView view) {
        int shakeDegrees = 20;
        //先往左再往右
        PropertyValuesHolder rotateValuesHolder = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_X,
                Keyframe.ofFloat(0f, 0f),
                Keyframe.ofFloat(0.1f, -shakeDegrees),
                Keyframe.ofFloat(0.2f, 0f),
                Keyframe.ofFloat(0.3f, shakeDegrees),
                Keyframe.ofFloat(0.4f, 0f),
                Keyframe.ofFloat(0.5f, -shakeDegrees),
                Keyframe.ofFloat(0.6f, 0f),
                Keyframe.ofFloat(0.7f, shakeDegrees),
                Keyframe.ofFloat(0.8f, 0f),
                Keyframe.ofFloat(0.9f, -shakeDegrees),
                Keyframe.ofFloat(1.0f, 0f));

        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(view, rotateValuesHolder);
        objectAnimator.setDuration(500);
        objectAnimator.start();

        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                        | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }



}
