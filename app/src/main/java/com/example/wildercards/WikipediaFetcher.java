package com.example.wildercards;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

public class WikipediaFetcher {

    private final OkHttpClient client = new OkHttpClient();

    public String getAnimalSummary(String animalName) {
        String apiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + animalName;

        try {
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "Error: " + response.code();
                }

                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);

                String title = json.optString("title", "Unknown");
                String description = json.optString("description", "No description");
                String extract = json.optString("extract", "No extract available");
                String thumbnail = json.has("thumbnail") ? json.getJSONObject("thumbnail").optString("source") : "";

                String funFact = extract.contains(".") ? extract.substring(0, extract.indexOf(".") + 1) : extract;

                return "Title: " + title + "\n"
                        + "Description: " + description + "\n"
                        + "Fun Fact: " + funFact + "\n"
                        + "Image: " + thumbnail;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching data";
        }
    }
}
