package com.balraksh.safkaro.ui.results;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.Collections;
import java.util.List;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.adapters.ResultGroupAdapter;
import com.balraksh.safkaro.data.MediaGroup;
import com.balraksh.safkaro.data.ScanResult;
import com.balraksh.safkaro.repository.CleanupPreferences;
import com.balraksh.safkaro.repository.ScanSessionStore;
import com.balraksh.safkaro.ui.BaseEdgeToEdgeActivity;
import com.balraksh.safkaro.ui.confirm.DeleteConfirmActivity;
import com.balraksh.safkaro.utils.FormatUtils;

public class ScanResultsActivity extends BaseEdgeToEdgeActivity {

    private boolean showingDuplicates = true;
    private ResultGroupAdapter adapter;
    private ScanResult scanResult;
    private TextView summaryText;
    private View emptyStateView;
    private MaterialButton deleteButton;
    private MaterialButton selectAllButton;
    private MaterialButton deselectAllButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_scan_results);

        scanResult = ScanSessionStore.getInstance().getCurrentResult();
        if (scanResult == null) {
            finish();
            return;
        }

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        summaryText = findViewById(R.id.textSummaryBanner);
        emptyStateView = findViewById(R.id.layoutEmptyState);
        deleteButton = findViewById(R.id.buttonDeleteSelected);
        selectAllButton = findViewById(R.id.buttonSelectAll);
        deselectAllButton = findViewById(R.id.buttonDeselectAll);

        RecyclerView recyclerView = findViewById(R.id.recyclerGroups);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultGroupAdapter(this::refreshUi);
        recyclerView.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showingDuplicates = tab != null && tab.getPosition() == 0;
                refreshUi();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        deleteButton.setOnClickListener(v -> {
            if (ScanSessionStore.getInstance().getSelectedCount() > 0) {
                startActivity(new Intent(this, DeleteConfirmActivity.class));
            }
        });
        selectAllButton.setOnClickListener(v -> {
            ScanSessionStore.getInstance().setSelectedForGroups(getCurrentGroups(), true);
            refreshUi();
        });
        deselectAllButton.setOnClickListener(v -> {
            ScanSessionStore.getInstance().setSelectedForGroups(getCurrentGroups(), false);
            refreshUi();
        });

        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        if (scanResult == null) {
            return;
        }
        List<? extends MediaGroup> groups = getCurrentGroups();
        summaryText.setText(getString(
                R.string.you_can_free,
                FormatUtils.formatStorage(ScanSessionStore.getInstance().getSelectedBytes())
        ));

        adapter.setGroups(groups == null ? Collections.emptyList() : groups);
        emptyStateView.setVisibility(groups == null || groups.isEmpty() ? View.VISIBLE : View.GONE);
        boolean hasGroups = groups != null && !groups.isEmpty();
        selectAllButton.setVisibility(hasGroups ? View.VISIBLE : View.GONE);
        deselectAllButton.setVisibility(hasGroups ? View.VISIBLE : View.GONE);
        selectAllButton.setEnabled(hasGroups && ScanSessionStore.getInstance().hasUnselectedItemsInGroups(groups));
        deselectAllButton.setEnabled(hasGroups && ScanSessionStore.getInstance().hasSelectedItemsInGroups(groups));

        int selectedCount = ScanSessionStore.getInstance().getSelectedCount();
        deleteButton.setEnabled(selectedCount > 0);
        deleteButton.setText(selectedCount > 0
                ? getString(R.string.delete_selected, selectedCount)
                : getString(R.string.nothing_selected));
        new CleanupPreferences(this).setLastScanPotentialBytes(ScanSessionStore.getInstance().getSelectedBytes());
    }

    private List<? extends MediaGroup> getCurrentGroups() {
        return showingDuplicates ? scanResult.getDuplicateGroups() : scanResult.getSimilarGroups();
    }
}
