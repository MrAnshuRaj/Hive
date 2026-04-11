package com.balraksh.safkaro.ui.video;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.ui.BaseEdgeToEdgeActivity;

public class VideoCompressionActivity extends BaseEdgeToEdgeActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_video_compression);
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }
}
