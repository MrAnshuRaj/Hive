package com.balraksh.hive.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import com.balraksh.hive.R;

public class HomeProfileBottomSheet extends BottomSheetDialogFragment {

    public interface Callbacks {
        void onOpenPremium();
        void onOpenSettings();
        void onOpenNotifications();
        void onOpenPrivacy();
        void onRateApp();
        void onShareApp();
        void onHelpAndSupport();
        void onResetPreferences();
    }

    private Callbacks callbacks;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Callbacks) {
            callbacks = (Callbacks) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_home_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.buttonCloseProfile).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.cardUpgradeHive).setOnClickListener(v -> dispatch(() -> callbacks.onOpenPremium()));
        view.findViewById(R.id.rowAccountSettings).setOnClickListener(v -> dispatch(() -> callbacks.onOpenSettings()));
        view.findViewById(R.id.rowNotifications).setOnClickListener(v -> dispatch(() -> callbacks.onOpenNotifications()));
        view.findViewById(R.id.rowPrivacy).setOnClickListener(v -> dispatch(() -> callbacks.onOpenPrivacy()));
        view.findViewById(R.id.rowRateHive).setOnClickListener(v -> dispatch(() -> callbacks.onRateApp()));
        view.findViewById(R.id.rowShareFriends).setOnClickListener(v -> dispatch(() -> callbacks.onShareApp()));
        view.findViewById(R.id.rowHelpSupport).setOnClickListener(v -> dispatch(() -> callbacks.onHelpAndSupport()));
        view.findViewById(R.id.buttonResetPreferences).setOnClickListener(v -> dispatch(() -> callbacks.onResetPreferences()));
    }

    private void dispatch(@NonNull Runnable action) {
        dismiss();
        if (callbacks != null) {
            action.run();
        }
    }
}
