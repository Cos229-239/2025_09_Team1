package com.example.wildercards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConfirmImageActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmImageActivity";

    private ImageView ivSelectedImage;
    private Button btnConfirm;
    private Uri imageUri;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_image);

        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        btnConfirm = findViewById(R.id.btnConfirm);

        String uriString = getIntent().getStringExtra("image_uri");
        if (uriString != null) {
            imageUri = Uri.parse(uriString);
            if (ivSelectedImage != null) {
                ivSelectedImage.setImageURI(imageUri);
            } else {
                Log.e(TAG, "ImageView is null. Cannot display image.");
            }
        } else {
            Log.e(TAG, "Image URI was null. Cannot display image.");
            Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show();
            finish();
        }

        btnConfirm.setOnClickListener(v -> {
            if (imageUri != null) {
                btnConfirm.setEnabled(false);
                Toast.makeText(ConfirmImageActivity.this, getString(R.string.identifying), Toast.LENGTH_SHORT).show();

                // Run image processing and network call on a background thread to prevent ANR
                new Thread(() -> {
                    try {
                        // This is a slow operation and must be off the main thread
                        identityImage(imageUri);
                    } catch (IOException e) {
                        Log.e(TAG, "Error processing image before network call", e);
                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmImageActivity.this, getString(R.string.error_identifying_image),
                                    Toast.LENGTH_SHORT).show();
                            btnConfirm.setEnabled(true);
                        });
                    }
                }).start();
            } else {
                Toast.makeText(ConfirmImageActivity.this, getString(R.string.no_image_selected),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void identityImage(Uri imageUri) throws IOException {
        // Part 1: Image processing (runs on the background thread from onClick)
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageBytes = stream.toByteArray();

        String apiKey = BuildConfig.GOOGLE_VISION_API;
        String apiUrl = "https://vision.googleapis.com/v1/images:annotate?key=" + apiKey;

        String jsonBody;
        try {
            JSONObject imageObject = new JSONObject();
            imageObject.put("content", Base64.encodeToString(imageBytes, Base64.DEFAULT));

            JSONArray featuresArray = new JSONArray();
            JSONObject labelFeature = new JSONObject();
            labelFeature.put("type", "LABEL_DETECTION");
            labelFeature.put("maxResults", 10);
            featuresArray.put(labelFeature);

            JSONObject webFeature = new JSONObject();
            webFeature.put("type", "WEB_DETECTION");
            webFeature.put("maxResults", 10);
            featuresArray.put(webFeature);

            JSONObject requestObject = new JSONObject();
            requestObject.put("image", imageObject);
            requestObject.put("features", featuresArray);

            JSONObject mainRequest = new JSONObject();
            mainRequest.put("requests", new JSONArray().put(requestObject));

            jsonBody = mainRequest.toString();

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request", e);
            // Since we are on a background thread, we can't show a toast directly
            // We can re-enable the button and the user can try again
            runOnUiThread(() -> btnConfirm.setEnabled(true));
            return;
        }

        RequestBody requestBody = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build();

        // Part 2: Network Call (OkHttp's enqueue handles its own background threading)
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Log the full exception to get more details on the network failure
                Log.e(TAG, "Google Vision API call failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmImageActivity.this, getString(R.string.api_call_failed), Toast.LENGTH_LONG).show();
                    btnConfirm.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body().string(); // Read body once

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response + " Body: " + responseData);
                    runOnUiThread(() -> {
                        Toast.makeText(ConfirmImageActivity.this, getString(R.string.api_error, response.code()), Toast.LENGTH_LONG).show();
                        btnConfirm.setEnabled(true);
                    });
                    return;
                }
                
                Log.d(TAG, "Response: " + responseData);

                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    JSONArray responsesArray = jsonObject.getJSONArray("responses");

                    if (responsesArray.length() > 0) {
                        JSONObject firstResponse = responsesArray.getJSONObject(0);
                        if (firstResponse.has("labelAnnotations")) {
                            JSONArray labelAnnotations = firstResponse.getJSONArray("labelAnnotations");
                            if (labelAnnotations.length() > 0) {
                                JSONObject topResult = labelAnnotations.getJSONObject(0);
                                String description = topResult.getString("description");

                                Log.d(TAG, "Top Label: " + description);

                                runOnUiThread(() -> {
                                    Toast.makeText(ConfirmImageActivity.this, getString(R.string.identified, description), Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(ConfirmImageActivity.this, ConfirmCardActivity.class);
                                    intent.putExtra("animal_name", description);
                                    startActivity(intent);
                                    finish(); // Finish this activity
                                });
                                return; 
                            }
                        }
                    }

                    Log.d(TAG, "No suitable annotations found in the response.");
                    runOnUiThread(() -> {
                        Toast.makeText(ConfirmImageActivity.this, getString(R.string.could_not_identify), Toast.LENGTH_LONG).show();
                        btnConfirm.setEnabled(true);
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON", e);
                    runOnUiThread(() -> {
                        Toast.makeText(ConfirmImageActivity.this, getString(R.string.error_parsing_results), Toast.LENGTH_LONG).show();
                        btnConfirm.setEnabled(true);
                    });
                }
            }
        });
    }
}
