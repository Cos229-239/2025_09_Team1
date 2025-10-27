package com.example.wildercards;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Fetches animal information from Wikipedia and Wikidata APIs.
 * This class uses standard Android libraries (org.json, java.net) - no external dependencies needed.
 */
public class WikipediaFetcher {
    private static final String TAG = "WikipediaFetcher";
    private static final int TIMEOUT = 15000; // 15 seconds

    /**
     * Fetches complete animal information including data from both Wikipedia and Wikidata.
     * @param animalName The name of the animal to search for
     * @return AnimalInfo object with all data, or null if fetch fails
     */
    public static AnimalInfo fetchAnimalInfo(String animalName) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "Starting fetch for: " + animalName);
        Log.d(TAG, "========================================");

        try {
            // STEP 1: Fetch Wikipedia summary
            String formattedName = animalName.trim().replace(" ", "_");
            String wikipediaUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                    URLEncoder.encode(formattedName, "UTF-8");

            Log.d(TAG, "Step 1: Fetching Wikipedia summary");
            Log.d(TAG, "URL: " + wikipediaUrl);

            JSONObject wikipediaData = fetchJsonFromUrl(wikipediaUrl);

            if (wikipediaData == null) {
                Log.e(TAG, "Failed to fetch Wikipedia data");
                return null;
            }

            // Extract Wikipedia data
            String title = wikipediaData.optString("title", animalName);
            String description = wikipediaData.optString("extract", "No description available");
            String imageUrl = "";

            if (wikipediaData.has("thumbnail")) {
                JSONObject thumbnail = wikipediaData.getJSONObject("thumbnail");
                imageUrl = thumbnail.optString("source", "");
            }

            // Handle cases where there's an original image but no thumbnail
            if (imageUrl.isEmpty() && wikipediaData.has("originalimage")) {
                JSONObject originalImage = wikipediaData.getJSONObject("originalimage");
                imageUrl = originalImage.optString("source", "");
            }

            Log.d(TAG, "Wikipedia data extracted:");
            Log.d(TAG, "  Title: " + title);
            Log.d(TAG, "  Description length: " + description.length());
            Log.d(TAG, "  Image URL: " + imageUrl);

            // STEP 2: Get Wikidata ID from Wikipedia response
            String wikidataId = wikipediaData.optString("wikibase_item", null);

            if (wikidataId == null || wikidataId.isEmpty()) {
                Log.w(TAG, "No Wikidata ID found, returning basic info only");
                return new AnimalInfo(title, "", description, imageUrl, "", "");
            }

            Log.d(TAG, "Step 2: Found Wikidata ID: " + wikidataId);

            // STEP 3: Fetch Wikidata information
            String wikidataUrl = "https://www.wikidata.org/wiki/Special:EntityData/" +
                    wikidataId + ".json";

            Log.d(TAG, "Step 3: Fetching Wikidata");
            Log.d(TAG, "URL: " + wikidataUrl);

            JSONObject wikidataResponse = fetchJsonFromUrl(wikidataUrl);

            if (wikidataResponse == null) {
                Log.w(TAG, "Failed to fetch Wikidata, returning basic info");
                return new AnimalInfo(title, "", description, imageUrl, "", "");
            }

            // STEP 4: Parse Wikidata
            String scientificName = "";
            String habitat = "";
            String conservationStatus = "";

            try {
                JSONObject entities = wikidataResponse.getJSONObject("entities");
                JSONObject entity = entities.getJSONObject(wikidataId);
                JSONObject claims = entity.getJSONObject("claims");

                Log.d(TAG, "Step 4: Parsing Wikidata claims");

                // Get scientific name (P225)
                if (claims.has("P225")) {
                    JSONArray scientificNameArray = claims.getJSONArray("P225");
                    if (scientificNameArray.length() > 0) {
                        JSONObject firstClaim = scientificNameArray.getJSONObject(0);
                        JSONObject mainsnak = firstClaim.getJSONObject("mainsnak");

                        if (mainsnak.has("datavalue")) {
                            JSONObject datavalue = mainsnak.getJSONObject("datavalue");
                            scientificName = datavalue.optString("value", "");
                            Log.d(TAG, "  Scientific name: " + scientificName);
                        }
                    }
                }

                // Get habitat (P2303 - natural habitat)
                if (claims.has("P2303")) {
                    habitat = extractWikidataLabel(claims.getJSONArray("P2303"), entities, "habitat");
                    Log.d(TAG, "  Habitat: " + habitat);
                }

                // Fallback: Try P2975 (endemic to) if P2303 not found
                if (habitat.isEmpty() && claims.has("P2975")) {
                    habitat = extractWikidataLabel(claims.getJSONArray("P2975"), entities, "endemic to") + " (endemic)";
                    Log.d(TAG, "  Habitat (endemic): " + habitat);
                }

                // Get conservation status (P141)
                if (claims.has("P141")) {
                    conservationStatus = extractWikidataLabel(claims.getJSONArray("P141"), entities, "conservation status");
                    Log.d(TAG, "  Conservation status: " + conservationStatus);
                }

            } catch (Exception e) {
                Log.w(TAG, "Error parsing Wikidata (continuing with partial data): " + e.getMessage());
            }

            // STEP 5: Create and return AnimalInfo
            AnimalInfo result = new AnimalInfo(
                    title,
                    scientificName.isEmpty() ? "Unknown" : scientificName,
                    description,
                    imageUrl,
                    habitat.isEmpty() ? "Unknown" : habitat,
                    conservationStatus.isEmpty() ? "Unknown" : conservationStatus
            );

            Log.d(TAG, "========================================");
            Log.d(TAG, "Fetch completed successfully!");
            Log.d(TAG, "========================================");

            return result;

        } catch (Exception e) {
            Log.e(TAG, "========================================");
            Log.e(TAG, "Fetch failed with exception:");
            Log.e(TAG, "Exception type: " + e.getClass().getName());
            Log.e(TAG, "Message: " + e.getMessage());
            Log.e(TAG, "========================================");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fetches JSON data from a URL using HttpURLConnection.
     * @param urlString The URL to fetch from
     * @return JSONObject containing the response, or null if fetch fails
     */
    private static JSONObject fetchJsonFromUrl(String urlString) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "WildercardsApp/1.0");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "  HTTP Response code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "  HTTP error: " + responseCode);
                return null;
            }

            // Read response
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            String jsonString = response.toString();
            Log.d(TAG, "  Response received: " + jsonString.length() + " bytes");

            return new JSONObject(jsonString);

        } catch (Exception e) {
            Log.e(TAG, "  Error fetching from URL: " + urlString);
            Log.e(TAG, "  Error: " + e.getMessage());
            return null;

        } finally {
            // Clean up resources
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing reader: " + e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Extracts a human-readable label from a Wikidata claim array.
     * Wikidata stores references as entity IDs (like Q12345), this method resolves them to labels.
     *
     * @param claimArray The JSONArray of claims
     * @param entities The entities object from the Wikidata response (for lookups)
     * @param fieldName Name of the field being extracted (for logging)
     * @return Human-readable label, or empty string if not found
     */
    private static String extractWikidataLabel(JSONArray claimArray, JSONObject entities, String fieldName) {
        try {
            if (claimArray.length() == 0) {
                return "";
            }

            JSONObject firstClaim = claimArray.getJSONObject(0);
            JSONObject mainsnak = firstClaim.getJSONObject("mainsnak");

            if (!mainsnak.has("datavalue")) {
                return "";
            }

            JSONObject datavalue = mainsnak.getJSONObject("datavalue");

            // Check if it's a string value (direct text)
            if (datavalue.getString("type").equals("string")) {
                return datavalue.optString("value", "");
            }

            // Check if it's a wikibase-entityid (reference to another entity)
            if (datavalue.getString("type").equals("wikibase-entityid")) {
                JSONObject value = datavalue.getJSONObject("value");
                String entityId = value.optString("id", "");

                if (entityId.isEmpty()) {
                    return "";
                }

                // Try to fetch the label for this entity ID
                String label = fetchEntityLabel(entityId);

                if (!label.isEmpty()) {
                    return label;
                }

                // Fallback: return the entity ID
                return entityId;
            }

        } catch (Exception e) {
            Log.w(TAG, "  Error extracting " + fieldName + ": " + e.getMessage());
        }

        return "";
    }

    /**
     * Fetches the English label for a Wikidata entity ID.
     * Makes a separate API call to get human-readable text.
     *
     * @param entityId The Wikidata entity ID (e.g., "Q12345")
     * @return The English label, or empty string if not found
     */
    private static String fetchEntityLabel(String entityId) {
        try {
            String labelUrl = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" +
                    entityId + "&props=labels&languages=en&format=json";

            JSONObject response = fetchJsonFromUrl(labelUrl);

            if (response == null) {
                return "";
            }

            JSONObject entities = response.getJSONObject("entities");
            JSONObject entity = entities.getJSONObject(entityId);

            if (entity.has("labels")) {
                JSONObject labels = entity.getJSONObject("labels");
                if (labels.has("en")) {
                    JSONObject enLabel = labels.getJSONObject("en");
                    String label = enLabel.optString("value", "");
                    Log.d(TAG, "    Resolved " + entityId + " to: " + label);
                    return label;
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "    Error fetching label for " + entityId + ": " + e.getMessage());
        }

        return "";
    }

    /**
     * Helper method to test the fetcher with a simple animal name.
     * Useful for debugging.
     */
    public static void test(String animalName) {
        Log.d(TAG, "\n\n========== TESTING WikipediaFetcher ==========\n");
        AnimalInfo info = fetchAnimalInfo(animalName);

        if (info != null) {
            Log.d(TAG, "\n========== RESULTS ==========");
            Log.d(TAG, "Name: " + info.getName());
            Log.d(TAG, "Scientific: " + info.getScientificName());
            Log.d(TAG, "Description: " + info.getDescription().substring(0, Math.min(100, info.getDescription().length())) + "...");
            Log.d(TAG, "Image: " + info.getImageUrl());
            Log.d(TAG, "Habitat: " + info.getHabitat());
            Log.d(TAG, "Status: " + info.getConservationStatus());
            Log.d(TAG, "========================================\n\n");
        } else {
            Log.e(TAG, "TEST FAILED: Returned null");
        }
    }
}