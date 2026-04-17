package com.balraksh.hive.ui.home;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.balraksh.hive.R;
import com.balraksh.hive.data.HomeDashboardData;
import com.balraksh.hive.data.QuickCleanupItem;
import com.balraksh.hive.data.ScanConfig;
import com.balraksh.hive.data.ScanMode;
import com.balraksh.hive.data.ScanResult;
import com.balraksh.hive.data.SmartInsightItem;
import com.balraksh.hive.repository.CleanupPreferences;
import com.balraksh.hive.repository.HomeDashboardRepository;
import com.balraksh.hive.repository.ScanSessionStore;
import com.balraksh.hive.repository.VideoCompressionPreferences;
import com.balraksh.hive.ui.BaseEdgeToEdgeActivity;
import com.balraksh.hive.ui.BottomNavController;
import com.balraksh.hive.ui.permission.PermissionActivity;
import com.balraksh.hive.ui.premium.PremiumActivity;
import com.balraksh.hive.ui.results.ScanResultsActivity;
import com.balraksh.hive.ui.scanning.ScanningActivity;
import com.balraksh.hive.ui.settings.SettingsActivity;
import com.balraksh.hive.ui.setup.ScanSetupActivity;
import com.balraksh.hive.ui.video.VideoSelectActivity;
import com.balraksh.hive.utils.FormatUtils;
import com.balraksh.hive.utils.PermissionHelper;

public class HomeActivity extends BaseEdgeToEdgeActivity implements HomeProfileBottomSheet.Callbacks {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable insightSwitcher = new Runnable() {
        @Override
        public void run() {
            if (smartInsights == null || smartInsights.isEmpty()) {
                return;
            }
            currentInsightIndex = (currentInsightIndex + 1) % smartInsights.size();
            insightsPager.setCurrentItem(currentInsightIndex, true);
            handler.postDelayed(this, 4200L);
        }
    };

    private HomeDashboardRepository dashboardRepository;
    private CleanupPreferences cleanupPreferences;

    private StorageHoneycombView honeycombView;
    private TextView heroUsedText;
    private TextView heroFootnoteText;

    private ViewPager2 insightsPager;
    private SmartInsightsPagerAdapter insightsPagerAdapter;
    private View[] indicatorDots;

    private TextView statSpaceFreed;
    private TextView statFilesOrganized;
    private TextView statVideosCompressed;

    private MaterialCardView duplicateCard;
    private MaterialCardView similarCard;
    private MaterialCardView largeVideosCard;
    private TextView duplicateCountText;
    private TextView similarCountText;
    private TextView largeVideosCountText;
    private TextView duplicateSpaceText;
    private TextView similarSpaceText;
    private TextView largeVideosSpaceText;

    private List<SmartInsightItem> smartInsights;
    private int currentInsightIndex;
    private boolean entrancePlayed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_home);
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

        dashboardRepository = new HomeDashboardRepository(this);
        cleanupPreferences = new CleanupPreferences(this);

        bindViews();
        bindClicks();
        BottomNavController.bind(this, BottomNavController.TAB_HOME);
        startAmbientAnimations();
        scheduleInsightRotation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboard();
        scheduleInsightRotation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(insightSwitcher);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        handler.removeCallbacksAndMessages(null);
    }

    private void bindViews() {
        honeycombView = findViewById(R.id.viewStorageHoneycomb);
        heroUsedText = findViewById(R.id.textHeroUsed);
        heroFootnoteText = findViewById(R.id.textHeroFootnote);

        insightsPager = findViewById(R.id.pagerSmartInsights);
        insightsPagerAdapter = new SmartInsightsPagerAdapter(this::handleInsightAction);
        insightsPager.setAdapter(insightsPagerAdapter);
        insightsPager.setOffscreenPageLimit(1);
        insightsPager.setPageTransformer((page, position) -> {
            float absPosition = Math.abs(position);
            page.setAlpha(0.82f + ((1f - absPosition) * 0.18f));
            page.setScaleY(0.94f + ((1f - absPosition) * 0.06f));
        });
        insightsPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentInsightIndex = position;
                updateIndicators();
                scheduleInsightRotation();
            }
        });
        RecyclerView pagerRecyclerView = (RecyclerView) insightsPager.getChildAt(0);
        pagerRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        indicatorDots = new View[]{
                findViewById(R.id.dotInsightOne),
                findViewById(R.id.dotInsightTwo),
                findViewById(R.id.dotInsightThree)
        };

        statSpaceFreed = findViewById(R.id.textStatSpaceFreedValue);
        statFilesOrganized = findViewById(R.id.textStatFilesValue);
        statVideosCompressed = findViewById(R.id.textStatVideosValue);

        duplicateCard = findViewById(R.id.cardQuickDuplicates);
        similarCard = findViewById(R.id.cardQuickSimilar);
        largeVideosCard = findViewById(R.id.cardQuickLargeVideos);
        duplicateCountText = findViewById(R.id.textQuickDuplicatesCount);
        similarCountText = findViewById(R.id.textQuickSimilarCount);
        largeVideosCountText = findViewById(R.id.textQuickLargeVideosCount);
        duplicateSpaceText = findViewById(R.id.textQuickDuplicatesSpace);
        similarSpaceText = findViewById(R.id.textQuickSimilarSpace);
        largeVideosSpaceText = findViewById(R.id.textQuickLargeVideosSpace);
    }

    private void bindClicks() {
        findViewById(R.id.buttonAvatar).setOnClickListener(v ->
                new HomeProfileBottomSheet().show(getSupportFragmentManager(), "home_profile"));
        duplicateCard.setOnClickListener(v -> openDuplicateFlow());
        similarCard.setOnClickListener(v -> openSimilarFlow());
        largeVideosCard.setOnClickListener(v -> openCompressVideos());

        insightsPager.getChildAt(0).setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                handler.removeCallbacks(insightSwitcher);
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                scheduleInsightRotation();
            }
            return false;
        });
    }

    private void loadDashboard() {
        executor.execute(() -> {
            HomeDashboardData data = dashboardRepository.load();
            runOnUiThread(() -> bindDashboard(data));
        });
    }

    private void bindDashboard(HomeDashboardData data) {
        heroUsedText.setText(getString(
                R.string.home_storage_used,
                FormatUtils.formatStorage(data.getUsedStorageBytes()),
                FormatUtils.formatStorage(data.getTotalStorageBytes())
        ));
        heroFootnoteText.setText(getString(
                R.string.home_storage_footnote,
                FormatUtils.formatStorage(data.getRecommendedFreeBytes())
        ));
        honeycombView.setData(
                data.getTotalStorageBytes(),
                data.getUsedStorageBytes(),
                data.getRecommendedFreeBytes(),
                data.getCategories()
        );

        statSpaceFreed.setText(FormatUtils.formatStorage(data.getWeeklySummary().getSpaceFreedBytes()));
        statFilesOrganized.setText(String.valueOf(data.getWeeklySummary().getFilesOrganized()));
        statVideosCompressed.setText(String.valueOf(data.getWeeklySummary().getVideosCompressed()));

        bindQuickCleanup(data.getQuickCleanupItems());
        smartInsights = data.getSmartInsights();
        currentInsightIndex = Math.min(currentInsightIndex, Math.max(0, smartInsights.size() - 1));
        insightsPagerAdapter.setItems(smartInsights);
        if (!smartInsights.isEmpty()) {
            insightsPager.setCurrentItem(currentInsightIndex, false);
        }
        updateIndicators();
        scheduleInsightRotation();

        if (!entrancePlayed) {
            entrancePlayed = true;
            playEntranceAnimations();
        }
    }

    private void bindQuickCleanup(List<QuickCleanupItem> items) {
        for (QuickCleanupItem item : items) {
            if (item.getType() == QuickCleanupItem.TYPE_DUPLICATES) {
                duplicateCountText.setText(getString(R.string.home_items_count, item.getCount()));
                duplicateSpaceText.setText(FormatUtils.formatStorage(item.getEstimatedBytes()));
            } else if (item.getType() == QuickCleanupItem.TYPE_SIMILAR) {
                similarCountText.setText(getString(R.string.home_items_count, item.getCount()));
                similarSpaceText.setText(FormatUtils.formatStorage(item.getEstimatedBytes()));
            } else if (item.getType() == QuickCleanupItem.TYPE_LARGE_VIDEOS) {
                largeVideosCountText.setText(getString(R.string.home_items_count, item.getCount()));
                largeVideosSpaceText.setText(FormatUtils.formatStorage(item.getEstimatedBytes()));
            }
        }
    }

    private void updateIndicators() {
        for (int i = 0; i < indicatorDots.length; i++) {
            indicatorDots[i].setBackgroundResource(
                    i == currentInsightIndex ? R.drawable.bg_home_indicator_active : R.drawable.bg_home_indicator_inactive
            );
        }
    }

    private void scheduleInsightRotation() {
        handler.removeCallbacks(insightSwitcher);
        if (smartInsights != null && smartInsights.size() > 1) {
            handler.postDelayed(insightSwitcher, 4200L);
        }
    }

    private void startAmbientAnimations() {
        startFloatingAnimation(findViewById(R.id.layoutBrand), 10f, 1f, 1.03f, 5200L);
    }

    private void startFloatingAnimation(View view, float translateY, float startScale, float endScale, long durationMs) {
        ObjectAnimator translationAnimator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, -translateY, translateY);
        translationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        translationAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        translationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        translationAnimator.setDuration(durationMs);

        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(view, View.SCALE_X, startScale, endScale);
        scaleXAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        scaleXAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        scaleXAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleXAnimator.setDuration(durationMs);

        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(view, View.SCALE_Y, startScale, endScale);
        scaleYAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        scaleYAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        scaleYAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleYAnimator.setDuration(durationMs);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translationAnimator, scaleXAnimator, scaleYAnimator);
        animatorSet.start();
    }

    private void playEntranceAnimations() {
        View heroCard = findViewById(R.id.cardStorageHero);
        View insightSection = findViewById(R.id.layoutInsightSection);
        View weekSection = findViewById(R.id.layoutWeekSection);
        View cleanupSection = findViewById(R.id.layoutQuickCleanupSection);

        View[] sections = new View[]{heroCard, insightSection, weekSection, cleanupSection};
        for (int i = 0; i < sections.length; i++) {
            View view = sections[i];
            view.setAlpha(0f);
            view.setTranslationY(dp(28));
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 110L)
                    .setDuration(360L)
                    .start();
        }

        View[] quickCards = new View[]{duplicateCard, similarCard, largeVideosCard};
        for (int i = 0; i < quickCards.length; i++) {
            View view = quickCards[i];
            view.setAlpha(0f);
            view.setTranslationY(dp(18));
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(260L + (i * 90L))
                    .setDuration(260L)
                    .start();
        }
    }

    private void handleInsightAction(@Nullable SmartInsightItem item) {
        if (item == null) {
            return;
        }
        if (item.getActionType() == SmartInsightItem.ACTION_COMPRESS_VIDEOS) {
            openCompressVideos();
        } else if (item.getActionType() == SmartInsightItem.ACTION_REMOVE_DUPLICATES) {
            openDuplicateFlow();
        }
    }

    private void openDuplicateFlow() {
        ScanResult currentResult = ScanSessionStore.getInstance().getCurrentResult();
        if (currentResult != null && currentResult.getDuplicateMatchCount() > 0) {
            openScanResults(0);
            return;
        }
        openCleanPhotos();
    }

    private void openSimilarFlow() {
        ScanResult currentResult = ScanSessionStore.getInstance().getCurrentResult();
        if (currentResult != null && currentResult.getSimilarMatchCount() > 0) {
            openScanResults(1);
            return;
        }
        openCleanPhotos();
    }

    private void openScanResults(int tabIndex) {
        Intent intent = new Intent(this, ScanResultsActivity.class);
        intent.putExtra(ScanResultsActivity.EXTRA_INITIAL_TAB_INDEX, tabIndex);
        startActivity(intent);
    }

    private void openCleanPhotos() {
        if (!ensurePermissions()) {
            return;
        }
        startActivity(new Intent(this, ScanSetupActivity.class));
    }

    private void openCompressVideos() {
        if (!ensurePermissions()) {
            return;
        }
        startActivity(new Intent(this, VideoSelectActivity.class));
    }

    private boolean ensurePermissions() {
        if (PermissionHelper.hasRequiredPermissions(this)) {
            return true;
        }
        startActivity(new Intent(this, PermissionActivity.class));
        return false;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    public void onOpenPremium() {
        startActivity(new Intent(this, PremiumActivity.class));
    }

    @Override
    public void onOpenSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void onOpenNotifications() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    @Override
    public void onOpenPrivacy() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_OPEN_SECTION, SettingsActivity.SECTION_PRIVACY);
        startActivity(intent);
    }

    @Override
    public void onRateApp() {
        Uri marketUri = Uri.parse("market://details?id=" + getPackageName());
        Intent intent = new Intent(Intent.ACTION_VIEW, marketUri);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())
            ));
        }
    }

    @Override
    public void onShareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(
                R.string.home_share_message,
                "https://play.google.com/store/apps/details?id=" + getPackageName()
        ));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.home_share_with)));
    }

    @Override
    public void onHelpAndSupport() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:support@safkaro.app"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.home_support_subject));
        startActivity(Intent.createChooser(emailIntent, getString(R.string.home_help_support)));
    }

    @Override
    public void onResetPreferences() {
        cleanupPreferences.clear();
        new VideoCompressionPreferences(this).clear();
        ScanSessionStore.getInstance().clearCurrentResult();
        Toast.makeText(this, R.string.home_reset_done, Toast.LENGTH_SHORT).show();
        loadDashboard();
    }
}
