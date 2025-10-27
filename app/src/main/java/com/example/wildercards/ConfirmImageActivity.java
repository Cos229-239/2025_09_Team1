package com.example.wildercards;

import android.app.Activity;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    // A list of generic terms to ignore for a more specific identification
    private final List<String> genericTerms = Arrays.asList(
            "animal", "rodent", "mammal", "bird", "fish", "insect", "reptile", "amphibian",
            "mouse", "rat", "fauna", "chordate", "wildlife", "vertebrate", "invertebrate"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        imageUri = result.getData().getData();
                        if (ivSelectedImage != null) {
                            ivSelectedImage.setImageURI(imageUri);
                        }
                    } else {
                        // If user cancels image selection, and there was no initial image, finish.
                        if (imageUri == null) {
                            Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });

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
            // No image URI passed, launch the image chooser.
            openImageChooser();
        }

        btnConfirm.setOnClickListener(v -> {
            if (imageUri != null) {
                btnConfirm.setEnabled(false);
                Toast.makeText(ConfirmImageActivity.this, getString(R.string.identifying), Toast.LENGTH_SHORT).show();

                new Thread(() -> {
                    try {
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

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void identityImage(Uri imageUri) throws IOException {
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
            runOnUiThread(() -> btnConfirm.setEnabled(true));
            return;
        }

        RequestBody requestBody = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Google Vision API call failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmImageActivity.this, getString(R.string.api_call_failed), Toast.LENGTH_LONG).show();
                    btnConfirm.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body().string();

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
                    String description = null;

                    if (responsesArray.length() > 0) {
                        JSONObject firstResponse = responsesArray.getJSONObject(0);
                        description = findSpecificDescription(firstResponse);
                    }

                    if (description != null) {
                        final String finalDescription = description;
                        final String capitalizedDescription = finalDescription.substring(0, 1).toUpperCase() + finalDescription.substring(1);
                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmImageActivity.this, getString(R.string.identified, capitalizedDescription), Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(ConfirmImageActivity.this, ConfirmCardActivity.class);
                            intent.putExtra("animal_name", capitalizedDescription);
                            startActivity(intent);
                            finish();
                        });
                        return;
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

    private String findSpecificDescription(JSONObject response) throws JSONException {
        // 1. Prioritize web entities
        if (response.has("webDetection")) {
            JSONObject webDetection = response.getJSONObject("webDetection");
            if (webDetection.has("webEntities")) {
                JSONArray webEntities = webDetection.getJSONArray("webEntities");
                for (int i = 0; i < webEntities.length(); i++) {
                    JSONObject entity = webEntities.getJSONObject(i);
                    if (entity.has("description")) {
                        String desc = entity.getString("description");
                        if (!genericTerms.contains(desc.toLowerCase())) {
                            Log.d(TAG, "Found specific Web Entity: " + desc);
                            return desc;
                        }
                    }
                }
            }

            // 2. Fallback to best guess labels
            if (webDetection.has("bestGuessLabels")) {
                JSONArray bestGuessLabels = webDetection.getJSONArray("bestGuessLabels");
                for (int i = 0; i < bestGuessLabels.length(); i++) {
                    JSONObject label = bestGuessLabels.getJSONObject(i);
                    if (label.has("label")) {
                        String lbl = label.getString("label");
                        if (!genericTerms.contains(lbl.toLowerCase())) {
                            Log.d(TAG, "Found specific Best Guess Label: " + lbl);
                            return lbl;
                        }
                    }
                }
            }
        }

        // 3. Fallback to label annotations
        if (response.has("labelAnnotations")) {
            JSONArray labelAnnotations = response.getJSONArray("labelAnnotations");
            for (int i = 0; i < labelAnnotations.length(); i++) {
                JSONObject label = labelAnnotations.getJSONObject(i);
                if (label.has("description")) {
                    String desc = label.getString("description");
                    if (!genericTerms.contains(desc.toLowerCase())) {
                        Log.d(TAG, "Found specific Label Annotation: " + desc);
                        return desc;
                    }
                }
            }
        }

        // 4. If nothing specific, take the first web entity as a last resort
        if (response.has("webDetection")) {
            JSONObject webDetection = response.getJSONObject("webDetection");
            if (webDetection.has("webEntities")) {
                JSONArray webEntities = webDetection.getJSONArray("webEntities");
                if (webEntities.length() > 0) {
                    return webEntities.getJSONObject(0).optString("description", null);
                }
            }
        }

        return null; // No description found
    }
}
