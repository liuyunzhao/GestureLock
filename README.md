# 演示 #
![](https://github.com/liuyunzhao/GestureLock/blob/master/gif/gi1.gif)
![](https://github.com/liuyunzhao/GestureLock/blob/master/gif/gi2.gif)
![](https://github.com/liuyunzhao/GestureLock/blob/master/gif/gi3.gif)
## [Demo下载](http://www.baidu.com) ##

# 使用 #
可以下载示例代码,以全面了解使用方法,分步使用如下
## 第一步 ##
将PatternLockView放到Xml布局中
```<com.android.patternlockview.PatternLockView
         android:id="@+id/pattern_lock_view"
         android:layout_width="300dp"
         android:layout_height="300dp"
         android:layout_gravity="center"
         app:correctDotStateColor="#FE9D7F"
         app:correctDotStrokeStateColor="#FFDED4"
         app:dotAnimationDuration="200"
         app:dotCount="3"
         app:dotNormalSize="22dp"
         app:dotSelectedSize="30dp"
         app:normalDotStateColor="#888888"
         app:pathWidth="10dp"
         app:correctLineStateColor="#FFDED4"
         app:wrongLineStateColor="#C9C9C9"
         app:wrongDotStateColor="#888888"
         app:wrongDotStrokeStateColor="#C9C9C9"
         /> ``
## 第二步 ##
获取PatternLockView控件并添加手势监听事件
  mPatternLockView = (PatternLockView) findViewById(R.id.pattern_lock_view);
        mPatternLockView.addPatternLockListener(mPatternLockViewListener);  
实现监听接口
  private PatternLockViewListener mPatternLockViewListener = new PatternLockViewListener() {
        @Override
        public void onStarted() {
            Log.d(getClass().getName(), "Pattern drawing started");
        }

        @Override
        public void onProgress(List<PatternLockView.Dot> progressPattern) {
            Log.d(getClass().getName(), "Pattern progress: " +
                    PatternLockUtils.patternToString(mPatternLockView, progressPattern));
        }

        @Override
        public void onComplete(List<PatternLockView.Dot> pattern) {
            Log.d(getClass().getName(), "Pattern complete: " +
                    PatternLockUtils.patternToString(mPatternLockView, pattern));
        }

        @Override
        public void onCleared() {
            Log.d(getClass().getName(), "Pattern has been cleared");
        }
    };  
这样就可以使用了
当然也可以删除手势监听mPatternLockView.removePatternLockListener(mPatternLockViewListener);
