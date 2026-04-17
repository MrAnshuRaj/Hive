package com.balraksh.hive.video;

public enum VideoBitrateOption {
    LOW(0.75f),
    MEDIUM(0.55f),
    HIGH(0.35f);

    private final float sourceMultiplier;

    VideoBitrateOption(float sourceMultiplier) {
        this.sourceMultiplier = sourceMultiplier;
    }

    public float getSourceMultiplier() {
        return sourceMultiplier;
    }
}

