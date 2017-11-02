package com.cn.liuyz.listdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.patternlockview.PatternLockView;
import com.android.patternlockview.listener.PatternLockViewListener;
import com.android.patternlockview.utils.PatternLockUtils;
import com.android.patternlockview.utils.ResourceUtils;

import java.util.List;

/**
 * Created by yunzhao.liu on 2017/10/25
 */
public class PatternLockFragment extends Fragment {

    private PatternLockView mPatternLockView;
    private TextView mPrompt;
    private Button mBtn1,mBtn2,mBtn3;
    private final static String ENTRANCE = "entrance";

    private int mEntrance;

    public static PatternLockFragment newInstance(int entrance) {
        PatternLockFragment fragment = new PatternLockFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ENTRANCE, entrance);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.activity_pattern_lock_fragment, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initData();
        initClick();
    }

    private void initView(View view) {
        mPrompt = view.findViewById(R.id.prompt);
        mPatternLockView = view.findViewById(R.id.pattern_lock_view);
        mBtn1 = view.findViewById(R.id.btn1);
        mBtn2 = view.findViewById(R.id.btn2);
        mBtn3 = view.findViewById(R.id.btn3);
    }

    private void initData() {
        mEntrance = getArguments().getInt(ENTRANCE, 0);
        mPrompt.setText("低于4个点为错误手势，大于4个点为正确手势");
        mPatternLockView.addPatternLockListener(mPatternLockViewListener);
        mPatternLockView.setTactileFeedbackEnabled(true);//设置触觉是否能震动
        mPatternLockView.resetPattern();//恢复初始位置
    }

    private void initClick() {
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickBtn1();
            }
        });
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickBtn2();
            }
        });
        mBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickBtn3();
            }
        });
    }

    /**
     * 隐藏模式
     */
    private void clickBtn1() {
        mPatternLockView.setInStealthMode(true);
        mPatternLockView.setEnabled(true);
        mPatternLockView.resetPattern();
    }

    /**
     * 不可用模式
     */
    private void clickBtn2() {
        mPatternLockView.setInStealthMode(false);
        mPatternLockView.setEnabled(false);
        mPatternLockView.resetPattern();
//        mPatternLockView.setInputEnabled(false);//这个方法也可以设置不可用
    }

    /**
     * 另一种模式
     */
    private void clickBtn3() {
        mPatternLockView.resetPattern();
        mPatternLockView.setInStealthMode(false);
        mPatternLockView.setEnabled(true);
        mPatternLockView.setRingPaint(4);
        mPatternLockView.setSinglePathWidth(5);
        mPatternLockView.setCorrectLineStateColor(ResourceUtils.getColor(getActivity(), R.color.col_FE9D7F));
        mPatternLockView.setWrongLineStateColor(ResourceUtils.getColor(getActivity(), R.color.col_888));
    }


    private PatternLockViewListener mPatternLockViewListener = new PatternLockViewListener() {

        @Override
        public void onStarted() {
            onLockViewStarted();
        }

        @Override
        public void onProgress(List<PatternLockView.Dot> progressPattern) {
            onLockViewProgress(progressPattern);
        }

        @Override
        public void onComplete(List<PatternLockView.Dot> pattern) {
            onLockViewComplete(pattern);
        }

        @Override
        public void onCleared() {
            onLockViewCleared();
        }
    };

    private void onLockViewStarted() {
        Log.d("liuyz", "手势开始");
    }

    private void onLockViewProgress(List<PatternLockView.Dot> progressPattern) {
        Log.d("liuyz", "手势进度: " +
                PatternLockUtils.patternToString(mPatternLockView, progressPattern));
    }

    private void onLockViewComplete(List<PatternLockView.Dot> pattern) {
        Log.d("liuyz", "手势最终密码: " +
                PatternLockUtils.patternToString(mPatternLockView, pattern));
        String password = PatternLockUtils.patternToString(mPatternLockView, pattern);
        if (password.length() < 4) {
            //设置错误模式
            mPatternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG);
            mPrompt.setText("连接4个点后显示的就是正常的颜色");
            PatternLockUtils.shock(mPrompt);
            return;
        }
    }

    private void onLockViewCleared() {
        Log.d("liuyz", "手势取消");
    }
}
