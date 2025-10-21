package com.example.wildercards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
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
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConfirmImageActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmImageActivity";

    private ImageView confirmImageView;
    private ImageView ivSelectedImage;
    private Button btnConfirm;
    private Uri imageUri;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_image);

        Log.d("ConfirmImage", "onCreate: called");

        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        btnConfirm = findViewById(R.id.btnConfirm);


        confirmImageView = findViewById(R.id.ivSelectedImage);

        Intent intent = getIntent();
        String imageUriString = intent.getStringExtra("image_uri");
        String imagePath = intent.getStringExtra("image_path");


        Log.d("ConfirmImage", "onCreate: extras: image_uri = " + imageUriString
                + ", image_path = " + imagePath);

        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            confirmImageView.setImageURI(imageUri);
            Log.d("ConfirmImage", "onCreate: setImageURI");
        } else if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            confirmImageView.setImageBitmap(bitmap);
            Log.d("ConfirmImage", "onCreate: setImageBitmap from path");
        }else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            Log.d("ConfirmImage", "onCreate: no image selected");


//        String uriString = getIntent().getStringExtra("image_uri");
//        if (uriString != null) {
//            imageUri = Uri.parse(uriString);
//            if (ivSelectedImage != null) {
//                ivSelectedImage.setImageURI(imageUri);
//            } else {
//                Log.e(TAG, "ImageView is null. Cannot display image.");
//            }
//        } else {
//            Log.e(TAG, "Image URI was null. Cannot display image.");
//            Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show();
//            finish();
//        }

        btnConfirm.setOnClickListener(v -> {
            if (imageUri != null) {
                try {
                    identityImage(imageUri);
                    Toast.makeText(ConfirmImageActivity.this, getString(R.string.identifying), Toast.LENGTH_SHORT).show();
                    btnConfirm.setEnabled(false);
                } catch (IOException e) {
                    Log.e(TAG, "Error identifying image", e);
                    Toast.makeText(ConfirmImageActivity.this, getString(R.string.error_identifying_image),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ConfirmImageActivity.this, getString(R.string.no_image_selected),
                        Toast.LENGTH_SHORT).show();
            }
        });
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
            // 1. Create the 'image' object containing the Base64 encoded data
            JSONObject imageObject = new JSONObject();
            imageObject.put("content", Base64.encodeToString(imageBytes, Base64.DEFAULT));

            // 2. Create the 'features' array
            JSONArray featuresArray = new JSONArray();

            // Add LABEL_DETECTION feature
            JSONObject labelFeature = new JSONObject();
            labelFeature.put("type", "LABEL_DETECTION");
            labelFeature.put("maxResults", 10);
            featuresArray.put(labelFeature);

            // Add WEB_DETECTION feature (optional, but good for context)
            JSONObject webFeature = new JSONObject();
            webFeature.put("type", "WEB_DETECTION");
            webFeature.put("maxResults", 10);
            featuresArray.put(webFeature);

            // 3. Create the main 'request' object that contains the image and features
            JSONObject requestObject = new JSONObject();
            requestObject.put("image", imageObject);
            requestObject.put("features", featuresArray);

            // 4. Create the top-level object which contains the 'requests' array
            JSONObject mainRequest = new JSONObject();
            mainRequest.put("requests", new JSONArray().put(requestObject));

            jsonBody = mainRequest.toString();

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request", e);
            return;
        }


        RequestBody requestBody = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build();
        Log.d(TAG, "Request Body: " + jsonBody);


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Google Vision API call failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmImageActivity.this, getString(R.string.api_call_failed),
                            Toast.LENGTH_LONG).show();
                    btnConfirm.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response + " Body: " + response.body().string());
                    runOnUiThread(() -> {
                        Toast.makeText(ConfirmImageActivity.this, getString(R.string.api_error, response.code()),
                                Toast.LENGTH_LONG).show();
                        btnConfirm.setEnabled(true);
                    });
                    return;
                }

                String responseData = response.body().string();
                Log.d(TAG, "Response: " + responseData);

                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    // The response contains a 'responses' array
                    JSONArray responsesArray = jsonObject.getJSONArray("responses");

                    if (responsesArray.length() > 0) {
                        JSONObject firstResponse = responsesArray.getJSONObject(0);

                        // Look for labelAnnotations for general image classification
                        if (firstResponse.has("labelAnnotations")) {
                            JSONArray labelAnnotations = firstResponse.getJSONArray("labelAnnotations");
                            if (labelAnnotations.length() > 0) {

                                JSONObject topResult = labelAnnotations.getJSONObject(0);
                                String description = topResult.getString("description");
                                float score = (float) topResult.getDouble("score");

                                Log.d(TAG, "Top Label: " + description + ", Score: " + score);

                                runOnUiThread(() -> {
                                    Toast.makeText(ConfirmImageActivity.this, getString(R.string.identified, description), Toast.LENGTH_LONG).show();

                                    // TODO: Change to navigate to result or "card" page
                                    // Example:
                                    // Intent intent = new Intent(ConfirmImageActivity.this, CardActivity.class);
                                    // intent.putExtra("animal_name", description);
                                    // startActivity(intent);
                                    btnConfirm.setEnabled(true);
                                });
                                return; // Found a result, no need to check others
                            }
                        }
                    }

                    // If no label annotations were found
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