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

public final class BottomNavController {

    public static final int TAB_HOME = 0;
    public static final int TAB_CLEAN = 1;

    private BottomNavController() {
    }

    public static void bind(@NonNull Activity activity, int selectedTab) {
        View home = activity.findViewById(R.id.navHome);
        View clean = activity.findViewById(R.id.navClean);
        if (home == null || clean == null) {
            return;
        }

        bindTab(activity, R.id.navHome, R.id.navHomeIndicator, R.id.navHomeIcon, R.id.navHomeLabel, selectedTab == TAB_HOME);
        bindTab(activity, R.id.navClean, R.id.navCleanIndicator, R.id.navCleanIcon, R.id.navCleanLabel, selectedTab == TAB_CLEAN);
        bindPassiveTab(activity, R.id.navSwipeIcon, R.id.navSwipeLabel);
        bindPassiveTab(activity, R.id.navCompressIcon, R.id.navCompressLabel);

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
