## 演示 ##
![](https://github.com/liuyunzhao/GestureLock/blob/master/gif/gi1.gif)
![](https://github.com/liuyunzhao/GestureLock/blob/master/gif/gi2.gif)
![](https://github.com/liuyunzhao/GestureLock/blob/master/gif/gi3.gif)
**[Demo下载](https://github.com/liuyunzhao/GestureLock/blob/master/gif/app-demo.apk)**
## 使用 ##
可以下载示例代码,以全面了解使用方法,分步使用如下

**第一步**

将PatternLockView放到Xml布局中

```
<com.android.patternlockview.PatternLockView
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
        />  
```

**第二步**

获取PatternLockView控件并添加手势监听事件

```
mPatternLockView =(PatternLockView)findViewById(R.id.pattern_lock_view);
mPatternLockView.addPatternLockListener(mPatternLockViewListener);  
```
**实现监听接口**

```
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
```
这样就可以使用了
当然您也可以删除手势监听

```
mPatternLockView.removePatternLockListener(mPatternLockViewListener);
```
## 自定义 ##
除了在Xml中设置属性外，您也可以通过JAVA编程方式更改视图的属性

```
        mPatternLockView.setDotCount();
        mPatternLockView.setDotNormalSize();
        mPatternLockView.setDotSelectedSize();

        mPatternLockView.setPathWidth();
        mPatternLockView.setInputEnabled();
        mPatternLockView.setDotAnimationDuration();
        mPatternLockView.setInStealthMode();

        mPatternLockView.setNormalDotStateColor();
        mPatternLockView.setCorrectDotStateColor();
        mPatternLockView.setCorrectLineStateColor();
        mPatternLockView.setCorrectDotStrokeColor();
        mPatternLockView.setWrongDotStateColor();
        mPatternLockView.setWrongLineStateColor();
        mPatternLockView.setWrongDotStrokeStateColor();

        mPatternLockView.setRingPaint();
```
**请您自己实现需要的属性**

## 贡献 ##
这个库是从Aritra Roy的PatternLockView获取并添加了一些改进使其更加灵活，如果您发现bug或想改进它的任何方面，可以自由地用拉请求进行贡献。