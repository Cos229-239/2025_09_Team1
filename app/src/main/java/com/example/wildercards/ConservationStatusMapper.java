package com.example.wildercards;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps conservation status to abbreviations and WilderCoin rewards
 */
public class ConservationStatusMapper {

    private static final String TAG = "ConservationMapper";

    // Map full text conservation status to abbreviations
    private static final Map<String, String> STATUS_TO_ABBREV = new HashMap<String, String>() {{
        // Extinct categories
        put("extinct", "EX");
        put("extinct in the wild", "EW");

        // Threatened categories
        put("critically endangered", "CR");
        put("endangered", "EN");
        put("vulnerable", "VU");

        // Lower risk categories
        put("near threatened", "NT");
        put("least concern", "LC");
        put("conservation dependent", "CD");

        // Other categories
        put("data deficient", "DD");
        put("not evaluated", "NE");
        put("lower risk", "LR");
    }};

    // Map abbreviations to WilderCoin values
    private static final Map<String, Integer> ABBREV_TO_COINS = new HashMap<String, Integer>() {{
        put("EX", 20);   // Extinct
        put("EW", 20);   // Extinct in the Wild
        put("CR", 20);   // Critically Endangered
        put("EN", 15);   // Endangered
        put("VU", 15);   // Vulnerable
        put("NT", 10);   // Near Threatened
        put("LC", 5);    // Least Concern
        put("DD", 5);    // Data Deficient
        put("NE", 5);    // Not Evaluated
        put("CD", 10);   // Conservation Dependent
        put("LR", 5);    // Lower Risk
    }};

    // Map abbreviations to cool rarity names
    private static final Map<String, String> ABBREV_TO_RARITY = new HashMap<String, String>() {{
        put("EX", "Legendary");
        put("EW", "Legendary");
        put("CR", "Ultra Rare");
        put("EN", "Rare");
        put("VU", "Rare");
        put("NT", "Uncommon");
        put("LC", "Common");
        put("DD", "Common");
        put("NE", "Common");
        put("CD", "Uncommon");
        put("LR", "Common");
    }};

    /**
     * Convert full conservation status text to abbreviation
     * Example: "Least Concern" -> "LC"
     *
     * @param fullText The full conservation status text
     * @return Abbreviation (e.g., "LC") or "LC" as default
     */
    public static String mapFullTextToAbbreviation(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            Log.w(TAG, "Empty conservation status, defaulting to LC");
            return "LC";
        }

        // Clean the input: lowercase, trim, remove "Status:" prefix if present
        String cleaned = fullText.toLowerCase().trim();

        // Remove common prefixes
        cleaned = cleaned.replace("status:", "").trim();
        cleaned = cleaned.replace("conservation status:", "").trim();
        cleaned = cleaned.replace("iucn status:", "").trim();

        Log.d(TAG, "Mapping status: '" + fullText + "' -> cleaned: '" + cleaned + "'");

        // Try exact match
        if (STATUS_TO_ABBREV.containsKey(cleaned)) {
            String abbrev = STATUS_TO_ABBREV.get(cleaned);
            Log.d(TAG, "Found exact match: " + abbrev);
            return abbrev;
        }

        // Try partial match (if status contains the key)
        for (Map.Entry<String, String> entry : STATUS_TO_ABBREV.entrySet()) {
            if (cleaned.contains(entry.getKey())) {
                String abbrev = entry.getValue();
                Log.d(TAG, "Found partial match: " + abbrev);
                return abbrev;
            }
        }

        // Default to Least Concern if unknown
        Log.w(TAG, "Unknown conservation status: '" + fullText + "', defaulting to LC");
        return "LC";
    }

    /**
     * Get WilderCoins reward for a conservation status abbreviation
     *
     * @param abbreviation Conservation status abbreviation (e.g., "LC", "EN")
     * @return Number of WilderCoins (5-20)
     */
    public static int getCoinsForStatus(String abbreviation) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return 5; // Default
        }

        String abbrev = abbreviation.toUpperCase().trim();

        if (ABBREV_TO_COINS.containsKey(abbrev)) {
            int coins = ABBREV_TO_COINS.get(abbrev);
            Log.d(TAG, "Coins for " + abbrev + ": " + coins);
            return coins;
        }

        // Default to 5 coins if unknown
        Log.w(TAG, "Unknown abbreviation: " + abbreviation + ", defaulting to 5 coins");
        return 5;
    }

    /**
     * Get WilderCoins directly from full conservation status text
     * Convenience method that combines mapping and coin calculation
     *
     * @param fullText Full conservation status text
     * @return Number of WilderCoins
     */
    public static int getCoinsForFullText(String fullText) {
        String abbrev = mapFullTextToAbbreviation(fullText);
        return getCoinsForStatus(abbrev);
    }

    /**
     * Get cool rarity name for display
     * Example: "LC" -> "Common", "CR" -> "Ultra Rare"
     *
     * @param abbreviation Conservation status abbreviation
     * @return Rarity name for display
     */
    public static String getRarityName(String abbreviation) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return "Common";
        }

        String abbrev = abbreviation.toUpperCase().trim();

        if (ABBREV_TO_RARITY.containsKey(abbrev)) {
            return ABBREV_TO_RARITY.get(abbrev);
        }

        return "Common";
    }

    /**
     * Get rarity color resource ID for UI
     *
     * @param abbreviation Conservation status abbreviation
     * @return Android color resource ID
     */
    public static int getRarityColor(String abbreviation) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return android.R.color.holo_green_light; // Common = Green
        }

        String abbrev = abbreviation.toUpperCase().trim();

        switch (abbrev) {
            case "EX":
            case "EW":
            case "CR":
                return android.R.color.holo_red_dark; // Legendary/Ultra Rare = Red
            case "EN":
            case "VU":
                return android.R.color.holo_orange_dark; // Rare = Orange
            case "NT":
            case "CD":
                return android.R.color.holo_orange_light; // Uncommon = Yellow/Light Orange
            default:
                return android.R.color.holo_green_light; // Common = Green
        }
    }

    /**
     * Get emoji for rarity level
     *
     * @param abbreviation Conservation status abbreviation
     * @return Emoji string
     */
    public static String getRarityEmoji(String abbreviation) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return "🟢"; // Common
        }

        String abbrev = abbreviation.toUpperCase().trim();

        switch (abbrev) {
            case "EX":
            case "EW":
                return "💀"; // Extinct
            case "CR":
                return "🔴"; // Critically Endangered
            case "EN":
            case "VU":
                return "🟠"; // Endangered/Vulnerable
            case "NT":
            case "CD":
                return "🟡"; // Near Threatened
            default:
                return "🟢"; // Least Concern
        }
    }

    /**
     * Create a formatted message for earning coins
     * Example: "Card saved! +15 WilderCoins earned! 🪙 (Rare)"
     *
     * @param fullText Conservation status full text
     * @return Formatted message
     */
    public static String getEarnedMessage(String fullText) {
        String abbrev = mapFullTextToAbbreviation(fullText);
        int coins = getCoinsForStatus(abbrev);
        String rarity = getRarityName(abbrev);
        String emoji = getRarityEmoji(abbrev);

        return String.format("Card saved! +%d WilderCoins earned! 🪙 (%s %s)",
                coins, emoji, rarity);
    }
}