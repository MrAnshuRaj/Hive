package com.balraksh.safkaro.ui.onboarding;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class OnboardingPagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragments = new ArrayList<>();
    private final PaywallFragment paywallFragment;

    public OnboardingPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        for (int i = 0; i < 6; i++) {
            fragments.add(OnboardingFeatureFragment.newInstance(i));
        }
        paywallFragment = new PaywallFragment();
        fragments.add(paywallFragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    public PaywallFragment getPaywallFragment() {
        return paywallFragment;
    }
}
