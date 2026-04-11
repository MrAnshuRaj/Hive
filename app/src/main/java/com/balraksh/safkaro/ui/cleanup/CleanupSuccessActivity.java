package com.balraksh.safkaro.ui.cleanup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.balraksh.safkaro.R;
import com.balraksh.safkaro.data.CleanupOutcome;
import com.balraksh.safkaro.repository.ScanSessionStore;
import com.balraksh.safkaro.ui.BaseEdgeToEdgeActivity;
import com.balraksh.safkaro.ui.home.HomeActivity;
import com.balraksh.safkaro.utils.FormatUtils;

public class CleanupSuccessActivity extends BaseEdgeToEdgeActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEdgeToEdgeContentView(R.layout.activity_cleanup_success);

        CleanupOutcome outcome = ScanSessionStore.getInstance().getLastOutcome();
        if (outcome == null) {
            finish();
            return;
        }

        TextView freedValueText = findViewById(R.id.textFreedValue);
        TextView noteText = findViewById(R.id.textCleanupNote);
        freedValueText.setText(FormatUtils.formatStorage(outcome.getFreedBytes()));

        if (outcome.getFailedCount() > 0) {
            noteText.setVisibility(View.VISIBLE);
            noteText.setText(getString(R.string.partial_delete_message, outcome.getFailedCount()));
        } else {
            noteText.setVisibility(View.GONE);
        }

        findViewById(R.id.buttonBackHome).setOnClickListener(v -> openHome());
    }

    @Override
    public void onBackPressed() {
        openHome();
    }

    private void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
