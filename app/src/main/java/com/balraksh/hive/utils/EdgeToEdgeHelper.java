package com.balraksh.hive.utils;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class EdgeToEdgeHelper {

    private EdgeToEdgeHelper() {
    }

    public static void enable(@NonNull AppCompatActivity activity) {
        EdgeToEdge.enable(activity);
    }

    public static void applySystemBarInsets(@NonNull View view) {
        final int startPadding = view.getPaddingStart();
        final int topPadding = view.getPaddingTop();
        final int endPadding = view.getPaddingEnd();
        final int bottomPadding = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (target, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            target.setPaddingRelative(
                    startPadding + systemBars.left,
                    topPadding + systemBars.top,
                    endPadding + systemBars.right,
                    bottomPadding + systemBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}

