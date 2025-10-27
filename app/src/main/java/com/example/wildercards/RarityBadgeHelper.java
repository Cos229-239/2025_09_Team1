package com.example.wildercards;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

/**
 * Helper class to add rarity badges to card views
 */
public class RarityBadgeHelper {

    /**
     * Apply rarity badge styling to a TextView
     *
     * @param badgeView The TextView to style as a badge
     * @param conservationStatus The conservation status (full text or abbreviation)
     */
    public static void applyRarityBadge(TextView badgeView, String conservationStatus) {
        if (badgeView == null) return;

        // Get abbreviation if needed
        String abbrev = ConservationStatusMapper.mapFullTextToAbbreviation(conservationStatus);
        String rarityName = ConservationStatusMapper.getRarityName(abbrev);
        String emoji = ConservationStatusMapper.getRarityEmoji(abbrev);
        int coins = ConservationStatusMapper.getCoinsForStatus(abbrev);

        // Set badge text
        badgeView.setText(emoji + " " + rarityName);
        badgeView.setVisibility(View.VISIBLE);

        // Set background color based on rarity
        int backgroundColor = getRarityBackgroundColor(abbrev);
        badgeView.setBackgroundColor(backgroundColor);

        // Set text color (white for dark backgrounds, black for light)
        badgeView.setTextColor(Color.WHITE);

        // Add padding
        int paddingPx = dpToPx(badgeView.getContext(), 6);
        badgeView.setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2);
    }

    /**
     * Get background color for rarity
     */
    private static int getRarityBackgroundColor(String abbreviation) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return Color.parseColor("#4CAF50"); // Green for common
        }

        String abbrev = abbreviation.toUpperCase().trim();

        switch (abbrev) {
            case "EX":
            case "EW":
                return Color.parseColor("#9C27B0"); // Purple for Legendary (Extinct)
            case "CR":
                return Color.parseColor("#F44336"); // Red for Ultra Rare (Critically Endangered)
            case "EN":
            case "VU":
                return Color.parseColor("#FF9800"); // Orange for Rare
            case "NT":
            case "CD":
                return Color.parseColor("#FFC107"); // Amber for Uncommon
            case "LC":
            case "DD":
            case "NE":
            case "LR":
            default:
                return Color.parseColor("#4CAF50"); // Green for Common
        }
    }

    /**
     * Get border color for rarity (lighter version)
     */
    public static int getRarityBorderColor(String abbreviation) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return Color.parseColor("#81C784"); // Light Green
        }

        String abbrev = abbreviation.toUpperCase().trim();

        switch (abbrev) {
            case "EX":
            case "EW":
                return Color.parseColor("#BA68C8"); // Light Purple
            case "CR":
                return Color.parseColor("#E57373"); // Light Red
            case "EN":
            case "VU":
                return Color.parseColor("#FFB74D"); // Light Orange
            case "NT":
            case "CD":
                return Color.parseColor("#FFD54F"); // Light Amber
            default:
                return Color.parseColor("#81C784"); // Light Green
        }
    }

    /**
     * Convert dp to pixels
     */
    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Create a simple badge text (for use without TextView)
     *
     * @param conservationStatus Conservation status
     * @return Formatted badge string like "🔴 Ultra Rare"
     */
    public static String getBadgeText(String conservationStatus) {
        String abbrev = ConservationStatusMapper.mapFullTextToAbbreviation(conservationStatus);
        String rarityName = ConservationStatusMapper.getRarityName(abbrev);
        String emoji = ConservationStatusMapper.getRarityEmoji(abbrev);

        return emoji + " " + rarityName;
    }
}