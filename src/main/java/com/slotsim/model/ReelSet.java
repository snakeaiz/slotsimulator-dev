package com.slotsim.model;

import java.util.List;

/**
 * Ordered reel strips, one per grid column (reel 0 is the leftmost).
 */
public record ReelSet(List<ReelStrip> reels) {

    public ReelSet {
        if (reels == null || reels.isEmpty()) {
            throw new IllegalArgumentException("Reel set must contain at least one reel");
        }
        reels = List.copyOf(reels);
    }

    public int size() {
        return reels.size();
    }

    public ReelStrip reel(int index) {
        return reels.get(index);
    }

    /**
     * Size of the full cycle, i.e. the number of distinct stop combinations.
     */
    public long cycleSize() {
        long size = 1;
        for (ReelStrip reel : reels) {
            size = Math.multiplyExact(size, reel.length());
        }
        return size;
    }
}
