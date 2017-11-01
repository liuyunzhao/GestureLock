package com.andrognito.patternlockview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Dimension;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.ResourceUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static com.andrognito.patternlockview.PatternLockView.PatternViewMode.AUTO_DRAW;
import static com.andrognito.patternlockview.PatternLockView.PatternViewMode.CORRECT;
import static com.andrognito.patternlockview.PatternLockView.PatternViewMode.NORMAL;
import static com.andrognito.patternlockview.PatternLockView.PatternViewMode.WRONG;

/**
 * 自定义手势View
 */
public class PatternLockView extends View {

    /**
     * 模式
     */
    @IntDef({NORMAL, CORRECT, AUTO_DRAW, WRONG})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PatternViewMode {
        //默认状态
        int NORMAL = -1;
        //绘制中状态 绘制正确正确
        int CORRECT = 0;
        //自动绘制，代码已删除
        int AUTO_DRAW = 1;
        //错误状态
        int WRONG = 2;
    }

    private static final int DEFAULT_PATTERN_DOT_COUNT = 3;//默认点数
    private static final int DEFAULT_DOT_ANIMATION_DURATION = 190;//点放大缩小时间
    private static final int DEFAULT_PATH_END_ANIMATION_DURATION = 100;//点与点之间线的连接时间

    private DotState[][] mDotStates;//每个点对应的对象
    private int mPatternSize;//点的总数
    private float mHitFactor = 0.6f;//点范围
    private static int sDotCount;//设置行 高点数

    /*在attrs中有详细注释*/
    private int mNormalDotStateColor;
    private int mCorrectLineStateColor;
    private int mWrongLineStateColor;
    private int mWrongDotStateColor;
    private int mWrongDotStrokeStateColor;
    private int mCorrectDotStateColor;
    private int mCorrectDotStrokeColor;
    private int mPathWidth;
    private int mDotNormalSize;
    private int mDotSelectedSize;
    private int mDotAnimationDuration;
    private int mPathEndAnimationDuration;

    private Paint mDotPaint;//画点的画笔
    private Paint mPathPaint;//点与点的连线
    private Paint mRingPaint;//外环

    private List<PatternLockViewListener> mPatternListeners;
    private ArrayList<Dot> mPattern;//被点击过点的集合
    private boolean[][] mPatternDrawLookup;//记录那个点被选中

    private float mInProgressX = -1;
    private float mInProgressY = -1;

    private int mPatternViewMode = PatternViewMode.NORMAL;
    private boolean mInputEnabled = true;//控制输入是否可以用，用自带的就行
    private boolean mInStealthMode = false;//false:正常模式 true：隐藏手势，但点会放大
    private boolean mEnableHapticFeedback = true;//false:不震动  true：震动
    private boolean mPatternInProgress = false;//当开始画的时候，置为true 抬起变为false

    private boolean isSelectDot = false;//false:没选中到点 true:选中到点

    private float mViewWidth;
    private float mViewHeight;

    private final Path mCurrentPath = new Path();
    private Interpolator mFastOutSlowInInterpolator;
    private Interpolator mLinearOutSlowInInterpolator;

    public PatternLockView(Context context) {
        this(context, null);
    }

    public PatternLockView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PatternLockView);
        try {
            sDotCount = typedArray.getInt(R.styleable.PatternLockView_dotCount,
                    DEFAULT_PATTERN_DOT_COUNT);
            mPathWidth = (int) typedArray.getDimension(R.styleable.PatternLockView_pathWidth,
                    ResourceUtils.getDimensionInPx(getContext(), R.dimen.pattern_lock_path_width));
            mNormalDotStateColor = typedArray.getColor(R.styleable.PatternLockView_normalDotStateColor,
                    ResourceUtils.getColor(getContext(), R.color.white));
            mCorrectDotStateColor = typedArray.getColor(R.styleable.PatternLockView_correctDotStateColor,
                    ResourceUtils.getColor(getContext(), R.color.white));
            mCorrectDotStrokeColor = typedArray.getColor(R.styleable.PatternLockView_correctDotStrokeStateColor,
                    ResourceUtils.getColor(getContext(), R.color.white));
            mCorrectLineStateColor = typedArray.getColor(R.styleable.PatternLockView_correctLineStateColor,
                    ResourceUtils.getColor(getContext(), R.color.white));
            mWrongLineStateColor = typedArray.getColor(R.styleable.PatternLockView_wrongLineStateColor,
                    ResourceUtils.getColor(getContext(), R.color.white));
            mWrongDotStateColor = typedArray.getColor(R.styleable.PatternLockView_wrongDotStateColor,
                    ResourceUtils.getColor(getContext(), R.color.white));
            mWrongDotStrokeStateColor = typedArray.getColor(R.styleable.PatternLockView_wrongDotStrokeStateColor,
                    ResourceUtils.getColor(getContext(), R.color.white));
            mDotNormalSize = (int) typedArray.getDimension(R.styleable.PatternLockView_dotNormalSize,
                    ResourceUtils.getDimensionInPx(getContext(), R.dimen.pattern_lock_dot_size));
            mDotSelectedSize = (int) typedArray.getDimension(R.styleable.PatternLockView_dotSelectedSize,
                    ResourceUtils.getDimensionInPx(getContext(), R.dimen.pattern_lock_dot_selected_size));
            mDotAnimationDuration = typedArray.getInt(R.styleable.PatternLockView_dotAnimationDuration,
                    DEFAULT_DOT_ANIMATION_DURATION);
            mPathEndAnimationDuration = typedArray.getInt(R.styleable.PatternLockView_pathEndAnimationDuration,
                    DEFAULT_PATH_END_ANIMATION_DURATION);
        } finally {
            typedArray.recycle();
        }

        mPatternSize = sDotCount * sDotCount;
        mPattern = new ArrayList<>(mPatternSize);
        mPatternDrawLookup = new boolean[sDotCount][sDotCount];

        mDotStates = new DotState[sDotCount][sDotCount];
        for (int i = 0; i < sDotCount; i++) {
            for (int j = 0; j < sDotCount; j++) {
                mDotStates[i][j] = new DotState();
                mDotStates[i][j].mSize = mDotNormalSize;
            }
        }

        mPatternListeners = new ArrayList<>();
        initView();
    }

    private void initView() {
        setClickable(true);
        mPathPaint = new Paint();
        mPathPaint.setAntiAlias(true);
        mPathPaint.setDither(true);
        mPathPaint.setColor(mCorrectLineStateColor);
        mPathPaint.setStyle(Paint.Style.STROKE);
        //当画笔样式为STROKE或FILL_OR_STROKE时，设置笔刷的图形样式，如圆形样式Cap.ROUND,或方形样式Cap.SQUARE
        mPathPaint.setStrokeJoin(Paint.Join.ROUND);
        //设置绘制时各图形的结合方式，如平滑效果等
        mPathPaint.setStrokeCap(Paint.Cap.ROUND);
        mPathPaint.setStrokeWidth(mPathWidth);

        mDotPaint = new Paint();
        mDotPaint.setAntiAlias(true);
        mDotPaint.setDither(true);

        mRingPaint = new Paint();
        mRingPaint.setAntiAlias(true);
        mRingPaint.setDither(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !isInEditMode()) {
            mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                    getContext(), android.R.interpolator.fast_out_slow_in);
            mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                    getContext(), android.R.interpolator.linear_out_slow_in);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("liuyz", "onDraw");
        ArrayList<Dot> pattern = mPattern;
        int patternSize = pattern.size();
        boolean[][] drawLookupTable = mPatternDrawLookup;

        Path currentPath = mCurrentPath;
        currentPath.rewind();

        /**
         * 判断是否是隐藏模式
         */
        boolean drawPath = !mInStealthMode;
        if (drawPath) {
            mPathPaint.setColor(getCurrentLineStateColor());
            boolean anyCircles = false;
            float lastX = 0f;
            float lastY = 0f;
            for (int i = 0; i < patternSize; i++) {
                Dot dot = pattern.get(i);

                //如果点没有被选中，则不需要跳出循环
                if (!drawLookupTable[dot.mRow][dot.mColumn]) {
                    return;
                }
                anyCircles = true;

                float centerX = getCenterXForColumn(dot.mColumn);
                float centerY = getCenterYForRow(dot.mRow);
                if (i != 0) {
                    DotState state = mDotStates[dot.mRow][dot.mColumn];
                    currentPath.rewind();
                    //当到第二个的时候，则lastX，lastY就是选中的第一个点的中心
                    currentPath.moveTo(lastX, lastY);
                    if (state.mLineEndX != Float.MIN_VALUE
                            && state.mLineEndY != Float.MIN_VALUE) {//主要用于自动绘制
                        currentPath.lineTo(state.mLineEndX, state.mLineEndY);
                    } else {//手动绘制时进入的
                        currentPath.lineTo(centerX, centerY);
                    }
                    //把线连接上
                    canvas.drawPath(currentPath, mPathPaint);
                }
                //当是第一个时，先拿到第一个的中心点，然后从第一个开始画线，后续再把下一个点的中心点赋值
                lastX = centerX;
                lastY = centerY;
            }

            //这里绘制最后一个点和和点连接的线
            if ((mPatternInProgress || mPatternViewMode == AUTO_DRAW) && anyCircles) {
                currentPath.rewind();
                currentPath.moveTo(lastX, lastY);
                currentPath.lineTo(mInProgressX, mInProgressY);

//                mPathPaint.setAlpha((int) (calculateLastSegmentAlpha(
//                        mInProgressX, mInProgressY, lastX, lastY) * 255f));
                canvas.drawPath(currentPath, mPathPaint);
            }
        }

        //循环画点
        for (int i = 0; i < sDotCount; i++) {
            float centerY = getCenterYForRow(i);//排
            for (int j = 0; j < sDotCount; j++) {
                DotState dotState = mDotStates[i][j];
                float centerX = getCenterXForColumn(j);
                float size = dotState.mSize * dotState.mScale;
                float translationY = dotState.mTranslateY;
                drawCircle(canvas, (int) centerX, (int) centerY + translationY,
                        size, drawLookupTable[i][j], dotState.mAlpha);
            }
        }
    }

    /**
     * 这里是最终的画点方法
     */
    private void drawCircle(Canvas canvas, float centerX, float centerY,
                            float size, boolean partOfPattern, float alpha) {
        //如果隐藏模式则不走
        if (partOfPattern && !isInStealthMode()) {
            mRingPaint.setColor(getCurrentDotStrokeColor(partOfPattern));
            canvas.drawCircle(centerX, centerY, size, mRingPaint);
        }

        mDotPaint.setColor(getCurrentDotStateColor(partOfPattern));
        mDotPaint.setAlpha((int) (alpha * 255));
        canvas.drawCircle(centerX, centerY, size / 2, mDotPaint);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        /**
         * 设置每一个点占据的大小
         */
        int adjustedWidth = width - getPaddingLeft() - getPaddingRight();
        mViewWidth = adjustedWidth / (float) sDotCount;

        int adjustedHeight = height - getPaddingTop() - getPaddingBottom();
        mViewHeight = adjustedHeight / (float) sDotCount;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mInputEnabled || !isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                return true;
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                //第一次按下如果没有选中，则滑动中画到点上不选中
                if (!isSelectDot) {
                    return true;
                }
                handleActionMove(event);
                return true;
            case MotionEvent.ACTION_CANCEL:
                mPatternInProgress = false;
                resetPattern();
                notifyPatternCleared();
                return true;
        }
        return false;
    }

    private void handleActionDown(MotionEvent event) {
        resetPattern();
        float x = event.getX();
        float y = event.getY();
        Dot hitDot = detectAndAddHit(x, y);
        if (hitDot != null) {
            mPatternInProgress = true;
            isSelectDot = true;
            mPatternViewMode = CORRECT;
            notifyPatternStarted();
        } else {
            mPatternInProgress = false;
            isSelectDot = false;
            notifyPatternCleared();
        }
        if (hitDot != null) {
            float startX = getCenterXForColumn(hitDot.mColumn);
            float startY = getCenterYForRow(hitDot.mRow);

            float widthOffset = mViewWidth / 2f;
            float heightOffset = mViewHeight / 2f;

            // 局部刷新其实没多大用，在onDraw里边还是所有代码都走
            invalidate((int) (startX - widthOffset),
                    (int) (startY - heightOffset),
                    (int) (startX + widthOffset), (int) (startY + heightOffset));
        }
        mInProgressX = x;
        mInProgressY = y;
    }

    private void handleActionMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        Dot hitDot = detectAndAddHit(x, y);
        int patternSize = mPattern.size();
        /**
         * 前面做了判断，所以当第一次没点击到点上，不会走move，也就不会走到这里
         */
        if (hitDot != null && patternSize == 1) {
            mPatternInProgress = true;
            notifyPatternStarted();
        }

        mInProgressX = event.getX();
        mInProgressY = event.getY();
        invalidate();
    }

    private void handleActionUp(MotionEvent event) {
        // 判断手势集合中是否有选中的点
        if (!mPattern.isEmpty()) {
            mPatternInProgress = false;
            notifyPatternDetected();
            //抬起的时候再绘制一次
            invalidate();
        }
    }

    /**
     * 根据x,y坐标确定是否点击到点上
     */
    private Dot detectAndAddHit(float x, float y) {
        final Dot dot = checkForNewHit(x, y);
        if (dot != null) {
            Dot fillInGapDot = null;
            final ArrayList<Dot> pattern = mPattern;
            if (!pattern.isEmpty()) {
                Dot lastDot = pattern.get(pattern.size() - 1);
                int dRow = dot.mRow - lastDot.mRow;
                int dColumn = dot.mColumn - lastDot.mColumn;

                int fillInRow = lastDot.mRow;
                int fillInColumn = lastDot.mColumn;

                //重新计算行
                /**
                 * 重新计算行
                 * 例如：从（0，1）直接到（2，1），想跳过（1，1）时，则通过此方法把第一行添加进去
                 */
                if (Math.abs(dRow) == 2 && Math.abs(dColumn) != 1) {
                    fillInRow = lastDot.mRow + ((dRow > 0) ? 1 : -1);
                }
                //重新计算列
                if (Math.abs(dColumn) == 2 && Math.abs(dRow) != 1) {
                    fillInColumn = lastDot.mColumn + ((dColumn > 0) ? 1 : -1);
                }
                //
                fillInGapDot = Dot.of(fillInRow, fillInColumn);
            }

            /**
             * 例如：从（0，1）直接到（2，1），想跳过（1，1）时，则通过此方法把第一行添加进去
             */
            if (fillInGapDot != null
                    && !mPatternDrawLookup[fillInGapDot.mRow][fillInGapDot.mColumn]) {
                addCellToPattern(fillInGapDot);
            }
            /**
             * 如果中间跳过一个点则先添加跳过的点
             * 如果没有则添加选中的点
             */
            addCellToPattern(dot);
            /**
             * 震动
             */
            if (mEnableHapticFeedback) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            return dot;
        }
        return null;
    }

    /**
     * 添加选中的点到集合中
     */
    private void addCellToPattern(Dot newDot) {
        mPatternDrawLookup[newDot.mRow][newDot.mColumn] = true;
        mPattern.add(newDot);
        startDotSelectedAnimation(newDot);
        notifyPatternProgress();
    }

    /**
     * 设置模式
     */
    private void startDotSelectedAnimation(Dot dot) {
        final DotState dotState = mDotStates[dot.mRow][dot.mColumn];
        if (mInStealthMode) {
            startSizeAnimation(mDotNormalSize, mDotSelectedSize, mDotAnimationDuration,
                    mLinearOutSlowInInterpolator, dotState, new Runnable() {

                        @Override
                        public void run() {
                            //点放大后又缩小
                            startSizeAnimation(mDotSelectedSize, mDotNormalSize, mDotAnimationDuration,
                                    mFastOutSlowInInterpolator, dotState, null);
                        }
                    });
        }
    }

    private void startSizeAnimation(float start, float end, long duration,
                                    Interpolator interpolator, final DotState state,
                                    final Runnable endRunnable) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(start, end);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                state.mSize = (Float) animation.getAnimatedValue();
                invalidate();
            }

        });
        if (endRunnable != null) {
            valueAnimator.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (endRunnable != null) {
                        endRunnable.run();
                    }
                }
            });
        }
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    /**
     * 检查是否滑动到点上
     */
    private Dot checkForNewHit(float x, float y) {
        final int rowHit = getRowHit(y);
        if (rowHit < 0) {
            return null;
        }
        final int columnHit = getColumnHit(x);
        if (columnHit < 0) {
            return null;
        }

        /**
         * 如果已经选中，则不再选中
         */
        if (mPatternDrawLookup[rowHit][columnHit]) {
            return null;
        }
        return Dot.of(rowHit, columnHit);
    }

    /**
     * 根据y坐标判断是否在某一个点的y坐标上
     * if (y >= hitTop && y <= hitTop + hitSize)这行给点加了一个范围，在此范围内都算点上了点
     */
    private int getRowHit(float y) {
        final float squareHeight = mViewHeight;
        float hitSize = squareHeight * mHitFactor;

        float offset = getPaddingTop() + (squareHeight - hitSize) / 2f;
        for (int i = 0; i < sDotCount; i++) {
            float hitTop = offset + squareHeight * i;
            if (y >= hitTop && y <= hitTop + hitSize) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 根据x坐标判断是否在某一个点的x坐标上
     * if (x >= hitLeft && x <= hitLeft + hitSize)这行给点加了一个范围，在此范围内都算点上了点
     */
    private int getColumnHit(float x) {
        final float squareWidth = mViewWidth;
        float hitSize = squareWidth * mHitFactor;

        float offset = getPaddingLeft() + (squareWidth - hitSize) / 2f;
        for (int i = 0; i < sDotCount; i++) {

            final float hitLeft = offset + squareWidth * i;
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取点的X坐标
     */
    private float getCenterXForColumn(int column) {
        return getPaddingLeft() + column * mViewWidth + mViewWidth / 2f;
    }

    /**
     * 获取点的Y坐标
     */
    private float getCenterYForRow(int row) {
        return getPaddingTop() + row * mViewHeight + mViewHeight / 2f;
    }

    /**
     * 获取当前连接线的颜色
     */
    private int getCurrentLineStateColor() {
        if (mPatternViewMode == NORMAL) {
            return mCorrectLineStateColor;
        } else if (mPatternViewMode == WRONG) {
            return mWrongLineStateColor;
        } else if (mPatternViewMode == CORRECT
                || mPatternViewMode == AUTO_DRAW) {
            return mCorrectLineStateColor;
        } else {
            throw new IllegalStateException("Unknown view mode " + mPatternViewMode);
        }
    }

    /**
     * 获取当前点的颜色
     */
    private int getCurrentDotStateColor(boolean partOfPattern) {
        if (!partOfPattern || mInStealthMode || mPatternViewMode == NORMAL) {
            return mNormalDotStateColor;//这里给默认值
        } else if (mPatternViewMode == WRONG) {
            return mWrongDotStateColor;
        } else if (mPatternViewMode == CORRECT
                || mPatternViewMode == AUTO_DRAW) {
            return mCorrectDotStateColor;
        } else {
            throw new IllegalStateException("Unknown view mode " + mPatternViewMode);
        }
    }

    /**
     * 获取点的外围的颜色
     */
    private int getCurrentDotStrokeColor(boolean partOfPattern) {
        if (!partOfPattern || mInStealthMode || mPatternViewMode == NORMAL) {
            return mCorrectDotStrokeColor;
        } else if (mPatternViewMode == WRONG) {
            return mWrongDotStrokeStateColor;
        } else if (mPatternViewMode == CORRECT
                || mPatternViewMode == AUTO_DRAW) {
            return mCorrectDotStrokeColor;
        } else {
            throw new IllegalStateException("Unknown view mode " + mPatternViewMode);
        }
    }

    /**
     * 点对象
     */
    public static class Dot {
        private int mRow;
        private int mColumn;
        private static Dot[][] sDots;

        static {
            sDots = new Dot[sDotCount][sDotCount];

            //初始化所有点
            for (int i = 0; i < sDotCount; i++) {
                for (int j = 0; j < sDotCount; j++) {
                    sDots[i][j] = new Dot(i, j);
                }
            }
        }

        private Dot(int row, int column) {
            checkRange(row, column);
            this.mRow = row;
            this.mColumn = column;
        }

        public int getId() {
            return mRow * sDotCount + mColumn;
        }

        public int getRow() {
            return mRow;
        }

        public int getColumn() {
            return mColumn;
        }

        /**
         * 同步拿到一个点
         */
        public static synchronized Dot of(int row, int column) {
            checkRange(row, column);
            return sDots[row][column];
        }

        /**
         * 根据id值拿到某一个点
         */
        public static synchronized Dot of(int id) {
            return of(id / sDotCount, id % sDotCount);
        }

        /**
         * 容错处理
         */
        private static void checkRange(int row, int column) {
            if (row < 0 || row > sDotCount - 1) {
                throw new IllegalArgumentException("mRow must be in range 0-"
                        + (sDotCount - 1));
            }
            if (column < 0 || column > sDotCount - 1) {
                throw new IllegalArgumentException("mColumn must be in range 0-"
                        + (sDotCount - 1));
            }
        }

        @Override
        public String toString() {
            return "(Row = " + mRow + ", Col = " + mColumn + ")";
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof Dot)
                return mColumn == ((Dot) object).mColumn
                        && mRow == ((Dot) object).mRow;
            return super.equals(object);
        }

        @Override
        public int hashCode() {
            int result = mRow;
            result = 31 * result + mColumn;
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Dot> getPattern() {
        return (List<Dot>) mPattern.clone();
    }

    @PatternViewMode
    public int getPatternViewMode() {
        return mPatternViewMode;
    }

    public boolean isInStealthMode() {
        return mInStealthMode;
    }

    public boolean isTactileFeedbackEnabled() {
        return mEnableHapticFeedback;
    }

    public boolean isInputEnabled() {
        return mInputEnabled;
    }

    public int getDotCount() {
        return sDotCount;
    }

    public int getNormalDotStateColor() {
        return mNormalDotStateColor;
    }

    public int getWrongDotStateColor() {
        return mWrongDotStateColor;
    }

    public int getCorrectDotStateColor() {
        return mCorrectDotStateColor;
    }

    public int getPathWidth() {
        return mPathWidth;
    }

    public int getDotNormalSize() {
        return mDotNormalSize;
    }

    public int getDotSelectedSize() {
        return mDotSelectedSize;
    }

    public int getPatternSize() {
        return mPatternSize;
    }

    public int getDotAnimationDuration() {
        return mDotAnimationDuration;
    }

    public int getPathEndAnimationDuration() {
        return mPathEndAnimationDuration;
    }

    /**
     * 设置正确 错误点 线的显示颜色
     */
    public void setViewMode(@PatternViewMode int patternViewMode) {
        mPatternViewMode = patternViewMode;
        if (patternViewMode == AUTO_DRAW) {
            if (mPattern.size() == 0) {
                throw new IllegalStateException(
                        "you must have a pattern to "
                                + "animate if you want to set the display mode to animate");
            }
            final Dot first = mPattern.get(0);
            mInProgressX = getCenterXForColumn(first.mColumn);
            mInProgressY = getCenterYForRow(first.mRow);
            clearPatternDrawLookup();
        }
        invalidate();
    }

    public void setDotCount(int dotCount) {
        sDotCount = dotCount;
        mPatternSize = sDotCount * sDotCount;
        mPattern = new ArrayList<>(mPatternSize);
        mPatternDrawLookup = new boolean[sDotCount][sDotCount];

        mDotStates = new DotState[sDotCount][sDotCount];
        for (int i = 0; i < sDotCount; i++) {
            for (int j = 0; j < sDotCount; j++) {
                mDotStates[i][j] = new DotState();
                mDotStates[i][j].mSize = mDotNormalSize;
            }
        }

        requestLayout();
        invalidate();
    }

    public void setNormalDotStateColor(@ColorInt int normalDotStateColor) {
        mNormalDotStateColor = normalDotStateColor;
    }

    public void setWrongDotStateColor(@ColorInt int wrongDotStateColor) {
        mWrongDotStateColor = wrongDotStateColor;
    }

    public void setCorrectDotStateColor(@ColorInt int correctDotStateColor) {
        mCorrectDotStateColor = correctDotStateColor;
    }

    public void setCorrectLineStateColor(@ColorInt int correctLineStateColor) {
        mCorrectLineStateColor = correctLineStateColor;
    }

    public void setWrongLineStateColor(@ColorInt int wrongLineStateColor) {
        mWrongLineStateColor = wrongLineStateColor;
    }

    public void setPathWidth(@Dimension int pathWidth) {
        mPathWidth = pathWidth;
        initView();
        invalidate();
    }

    public void setSinglePathWidth(@Dimension int pathWidth) {
        mPathWidth = pathWidth;
        mPathPaint.setStrokeWidth(mPathWidth);
    }

    public void setDotNormalSize(@Dimension int dotNormalSize) {
        mDotNormalSize = dotNormalSize;

        for (int i = 0; i < sDotCount; i++) {
            for (int j = 0; j < sDotCount; j++) {
                mDotStates[i][j] = new DotState();
                mDotStates[i][j].mSize = mDotNormalSize;
            }
        }

        invalidate();
    }

    public void setDotSelectedSize(@Dimension int dotSelectedSize) {
        mDotSelectedSize = dotSelectedSize;
    }

    public void setDotAnimationDuration(int dotAnimationDuration) {
        mDotAnimationDuration = dotAnimationDuration;
        invalidate();
    }

    public void setPathEndAnimationDuration(int pathEndAnimationDuration) {
        mPathEndAnimationDuration = pathEndAnimationDuration;
    }

    /**
     * 设置是否显示隐藏手势
     */
    public void setInStealthMode(boolean inStealthMode) {
        mInStealthMode = inStealthMode;
    }

    public void setTactileFeedbackEnabled(boolean tactileFeedbackEnabled) {
        mEnableHapticFeedback = tactileFeedbackEnabled;
    }

    /**
     * 禁用任何输入
     */
    public void setInputEnabled(boolean inputEnabled) {
        mInputEnabled = inputEnabled;
    }

    public void setEnableHapticFeedback(boolean enableHapticFeedback) {
        mEnableHapticFeedback = enableHapticFeedback;
    }

    /**
     * 设置外环形状   可自定义
     */
    public void setRingPaint(int width) {
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeJoin(Paint.Join.ROUND);
        mRingPaint.setStrokeCap(Paint.Cap.ROUND);
        mRingPaint.setStrokeWidth(width);
    }

    public void addPatternLockListener(PatternLockViewListener patternListener) {
        mPatternListeners.add(patternListener);
    }

    public void removePatternLockListener(PatternLockViewListener patternListener) {
        mPatternListeners.remove(patternListener);
    }

    public void clearPattern() {
        resetPattern();
    }

    private void notifyPatternProgress() {
        notifyListenersProgress(mPattern);
    }

    private void notifyPatternStarted() {
        notifyListenersStarted();
    }

    private void notifyPatternDetected() {
        notifyListenersComplete(mPattern);
    }

    private void notifyPatternCleared() {
        notifyListenersCleared();
    }

    /**
     * 重置
     */
    public void resetPattern() {
        mPattern.clear();
        clearPatternDrawLookup();
        mPatternViewMode = NORMAL;
        invalidate();
    }


    private void notifyListenersStarted() {
        for (PatternLockViewListener patternListener : mPatternListeners) {
            if (patternListener != null) {
                patternListener.onStarted();
            }
        }
    }

    private void notifyListenersProgress(List<Dot> pattern) {
        for (PatternLockViewListener patternListener : mPatternListeners) {
            if (patternListener != null) {
                patternListener.onProgress(pattern);
            }
        }
    }

    private void notifyListenersComplete(List<Dot> pattern) {
        for (PatternLockViewListener patternListener : mPatternListeners) {
            if (patternListener != null) {
                patternListener.onComplete(pattern);
            }
        }
    }

    private void notifyListenersCleared() {
        for (PatternLockViewListener patternListener : mPatternListeners) {
            if (patternListener != null) {
                patternListener.onCleared();
            }
        }
    }

    private void clearPatternDrawLookup() {
        for (int i = 0; i < sDotCount; i++) {
            for (int j = 0; j < sDotCount; j++) {
                mPatternDrawLookup[i][j] = false;
            }
        }
    }

    public static class DotState {
        float mScale = 1.0f;
        float mTranslateY = 0.0f;
        float mAlpha = 1.0f;
        float mSize;
        float mLineEndX = Float.MIN_VALUE;
        float mLineEndY = Float.MIN_VALUE;
    }
}
