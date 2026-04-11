package com.balraksh.safkaro.ui.setup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.data.BucketOption;
import com.balraksh.safkaro.data.ScanConfig;
import com.balraksh.safkaro.data.ScanMode;
import com.balraksh.safkaro.repository.MediaRepository;
import com.balraksh.safkaro.ui.BaseEdgeToEdgeActivity;
import com.balraksh.safkaro.ui.scanning.ScanningActivity;

public class ScanSetupActivity extends BaseEdgeToEdgeActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ScanMode selectedMode = ScanMode.ALL_IMAGES;
    private long selectedBucketId = ScanConfig.NO_BUCKET_SELECTED;
    private String selectedBucketName;

    private MaterialCardView allImagesCard;
    private MaterialCardView folderCard;
    private MaterialCardView screenshotsCard;
    private TextView folderSubtitle;
    private MaterialSwitch duplicatesSwitch;
    private MaterialSwitch similarSwitch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_scan_setup);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        allImagesCard = findViewById(R.id.cardAllImages);
        folderCard = findViewById(R.id.cardSelectFolder);
        screenshotsCard = findViewById(R.id.cardScreenshots);
        folderSubtitle = findViewById(R.id.textSelectFolderSubtitle);
        duplicatesSwitch = findViewById(R.id.switchDuplicates);
        similarSwitch = findViewById(R.id.switchSimilar);

        allImagesCard.setOnClickListener(v -> {
            selectedMode = ScanMode.ALL_IMAGES;
            bindSelectionCards();
        });
        folderCard.setOnClickListener(v -> loadAndChooseBucket());
        screenshotsCard.setOnClickListener(v -> {
            selectedMode = ScanMode.SCREENSHOTS_ONLY;
            bindSelectionCards();
        });
        findViewById(R.id.buttonStartScan).setOnClickListener(v -> startScan());
        bindSelectionCards();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void loadAndChooseBucket() {
        executor.execute(() -> {
            List<BucketOption> options = new MediaRepository(this).loadBuckets();
            runOnUiThread(() -> {
                if (options.isEmpty()) {
                    Toast.makeText(this, R.string.no_folders_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] titles = new String[options.size()];
                int selectedIndex = 0;
                for (int i = 0; i < options.size(); i++) {
                    titles[i] = options.get(i).toString();
                    if (options.get(i).getBucketId() == selectedBucketId) {
                        selectedIndex = i;
                    }
                }
                final int[] chosenIndex = {selectedIndex};
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.choose_folder_prompt)
                        .setSingleChoiceItems(titles, selectedIndex, (dialog, which) -> chosenIndex[0] = which)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            BucketOption bucket = options.get(chosenIndex[0]);
                            selectedBucketId = bucket.getBucketId();
                            selectedBucketName = bucket.getBucketName();
                            selectedMode = ScanMode.SELECTED_BUCKET;
                            bindSelectionCards();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            });
        });
    }

    private void bindSelectionCards() {
        bindCardState(allImagesCard, selectedMode == ScanMode.ALL_IMAGES);
        bindCardState(folderCard, selectedMode == ScanMode.SELECTED_BUCKET);
        bindCardState(screenshotsCard, selectedMode == ScanMode.SCREENSHOTS_ONLY);
        if (selectedBucketName != null) {
            folderSubtitle.setText(getString(R.string.selected_bucket_name, selectedBucketName));
        } else {
            folderSubtitle.setText(R.string.select_folder_subtitle);
        }
    }

    private void bindCardState(MaterialCardView cardView, boolean selected) {
        cardView.setStrokeWidth(selected ? getResources().getDimensionPixelSize(R.dimen.space_2) : 1);
        cardView.setStrokeColor(getColor(selected ? R.color.color_primary : R.color.color_border));
        cardView.setCardBackgroundColor(getColor(selected ? R.color.color_primary_light : R.color.color_surface));
    }

    private void startScan() {
        if (!duplicatesSwitch.isChecked() && !similarSwitch.isChecked()) {
            Toast.makeText(this, R.string.enable_at_least_one_detection, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedMode == ScanMode.SELECTED_BUCKET
                && (selectedBucketName == null || selectedBucketName.trim().isEmpty())) {
            Toast.makeText(this, R.string.select_a_folder_first, Toast.LENGTH_SHORT).show();
            return;
        }
        ScanConfig config = new ScanConfig(
                selectedMode,
                duplicatesSwitch.isChecked(),
                similarSwitch.isChecked(),
                selectedBucketId,
                selectedBucketName
        );
        Intent intent = new Intent(this, ScanningActivity.class);
        config.writeToIntent(intent);
        startActivity(intent);
    }
}
