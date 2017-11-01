package com.cn.liuyz.listdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pattern_lock);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, PatternLockFragment.newInstance(2)).commit();
    }
}
