package com.balraksh.hive.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.balraksh.hive.utils.EdgeToEdgeHelper;

public abstract class BaseEdgeToEdgeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdgeHelper.enable(this);
        super.onCreate(savedInstanceState);
    }

    protected void setEdgeToEdgeContentView(@LayoutRes int layoutResId) {
        setContentView(layoutResId);
        View content = findViewById(android.R.id.content);
        EdgeToEdgeHelper.applySystemBarInsets(content);
    }
}

