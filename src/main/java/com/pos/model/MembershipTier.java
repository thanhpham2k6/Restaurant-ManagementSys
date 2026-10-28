package com.pos.model;

public enum MembershipTier {
    STANDARD("Thường", 0, 0),
    SILVER("Bạc", 2000000, 200),
    GOLD("Vàng", 5000000, 500),
    DIAMOND("Kim cương", 10000000, 1000);

    private final String displayName;
    private final long minSpent;
    private final int minPoints;

    MembershipTier(String displayName, long minSpent, int minPoints) {
        this.displayName = displayName;
        this.minSpent = minSpent;
        this.minPoints = minPoints;
    }

    public String getDisplayName() { return displayName; }
    public long getMinSpent() { return minSpent; }
    public int getMinPoints() { return minPoints; }

    public static MembershipTier fromTotalSpent(long totalSpent) {
        if (totalSpent >= DIAMOND.minSpent) return DIAMOND;
        if (totalSpent >= GOLD.minSpent) return GOLD;
        if (totalSpent >= SILVER.minSpent) return SILVER;
        return STANDARD;
    }

    public static MembershipTier fromPoints(int points) {
        if (points >= DIAMOND.minPoints) return DIAMOND;
        if (points >= GOLD.minPoints) return GOLD;
        if (points >= SILVER.minPoints) return SILVER;
        return STANDARD;
    }

    @Override
    public String toString() { return displayName; }
}
