// package com.example.wildercards.network;
package com.example.wildercards;
// imports
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeepAI {

    private static DeepAI instance;
    private final OkHttpClient client;
    private final String apiKey;
    private final Handler mainHandler;
    private static final String ENDPOINT = "https://api.deepai.org/api/image-editor";
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

    private DeepAI(String apiKey) {
        this.client = new OkHttpClient();
        this.apiKey = apiKey;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // singleton
    public static synchronized DeepAI getInstance(String apiKey) {
        if (instance == null) instance = new DeepAI(apiKey);
        return instance;
    }

    // Callback interface
    public interface ResultCallback {
        void onSuccess(String imageUrl);          // returns public URL of generated image
        void onError(String errorMessage);
    }

    public void editImage(Bitmap bitmap, String prompt, ResultCallback callback) {
        // convert Bitmap -> PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageBytes = baos.toByteArray();

        RequestBody imageBody = RequestBody.create(imageBytes, MEDIA_TYPE_PNG);

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.png", imageBody)
                .addFormDataPart("text", prompt) // your background prompt
                .build();

        Request request = new Request.Builder()
                .url(ENDPOINT)
                .post(requestBody)
                .addHeader("api-key", apiKey)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "empty";
                    final String err = "HTTP " + response.code() + ": " + body;
                    mainHandler.post(() -> callback.onError(err));
                    return;
                }

                String respString = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(respString);
                    String imageUrl = null;

                    // common DeepAI fields: "output_url" or "output" (array)
                    if (json.has("output_url")) {
                        imageUrl = json.getString("output_url");
                    } else if (json.has("output")) {
                        Object out = json.get("output");
                        if (out instanceof JSONArray) {
                            JSONArray arr = json.getJSONArray("output");
                            if (arr.length() > 0) imageUrl = arr.getString(0);
                        } else {
                            imageUrl = json.getString("output");
                        }
                    } else if (json.has("id")) {
                        // Sometimes you might get job id — you can form job-view-file URL:
                        // https://api.deepai.org/job-view-file/<ID>/outputs/output.jpg
                        String id = json.getString("id");
                        imageUrl = "https://api.deepai.org/job-view-file/" + id + "/outputs/output.jpg";
                    }

                    final String finalUrl = imageUrl;
                    if (finalUrl != null && !finalUrl.isEmpty()) {
                        mainHandler.post(() -> callback.onSuccess(finalUrl));
                    } else {
                        mainHandler.post(() -> callback.onError("No output URL in response: " + respString));
                    }

                } catch (JSONException e) {
                    mainHandler.post(() -> callback.onError("JSON error: " + e.getMessage() + " — raw: " + respString));
                }
            }
        });
    }
}

