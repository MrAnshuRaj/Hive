package com.balraksh.hive.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.balraksh.hive.R;
import com.balraksh.hive.ui.home.HomeActivity;
import com.balraksh.hive.ui.setup.ScanSetupActivity;
import com.balraksh.hive.ui.video.VideoSelectActivity;

public final class BottomNavController {

    public static final int TAB_HOME = 0;
    public static final int TAB_CLEAN = 1;
    public static final int TAB_COMPRESS = 3;

    private BottomNavController() {
    }

    public static void bind(@NonNull Activity activity, int selectedTab) {
        View home = activity.findViewById(R.id.navHome);
        View clean = activity.findViewById(R.id.navClean);
        View compress = activity.findViewById(R.id.navCompress);
        if (home == null || clean == null || compress == null) {
            return;
        }

        bindTab(activity, R.id.navHome, R.id.navHomeIndicator, R.id.navHomeIcon, R.id.navHomeLabel, selectedTab == TAB_HOME);
        bindTab(activity, R.id.navClean, R.id.navCleanIndicator, R.id.navCleanIcon, R.id.navCleanLabel, selectedTab == TAB_CLEAN);
        bindTab(activity, R.id.navCompress, R.id.navCompressIndicator, R.id.navCompressIcon, R.id.navCompressLabel, selectedTab == TAB_COMPRESS);
        bindPassiveTab(activity, R.id.navSwipeIcon, R.id.navSwipeLabel);

        home.setOnClickListener(v -> {
            if (selectedTab == TAB_HOME) {
                return;
            }
            Intent intent = new Intent(activity, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
            activity.finish();
        });

        clean.setOnClickListener(v -> {
            if (selectedTab == TAB_CLEAN) {
                return;
            }
            Intent intent = new Intent(activity, ScanSetupActivity.class);
            activity.startActivity(intent);
        });

        compress.setOnClickListener(v -> {
            if (selectedTab == TAB_COMPRESS) {
                return;
            }
            activity.startActivity(createCompressIntent(activity));
        });
    }

    @NonNull
    public static Intent createCompressIntent(@NonNull Activity activity) {
        Intent intent = new Intent(activity, VideoSelectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    private static void bindTab(
            @NonNull Activity activity,
            @IdRes int tabId,
            @IdRes int indicatorId,
            @IdRes int iconId,
            @IdRes int labelId,
            boolean selected
    ) {
        View tab = activity.findViewById(tabId);
        View indicator = activity.findViewById(indicatorId);
        ImageView icon = activity.findViewById(iconId);
        TextView label = activity.findViewById(labelId);
        if (tab == null || indicator == null || icon == null || label == null) {
            return;
        }

        int activeColor = activity.getColor(R.color.color_scan_gold);
        int inactiveColor = activity.getColor(R.color.color_scan_text_secondary);
        indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        icon.setColorFilter(selected ? activeColor : inactiveColor);
        label.setTextColor(selected ? activeColor : inactiveColor);
    }

    private static void bindPassiveTab(@NonNull Activity activity, @IdRes int iconId, @IdRes int labelId) {
        ImageView icon = activity.findViewById(iconId);
        TextView label = activity.findViewById(labelId);
        if (icon == null || label == null) {
            return;
        }
        int color = activity.getColor(R.color.color_scan_text_secondary);
        icon.setColorFilter(color);
        label.setTextColor(color);
    }
}
