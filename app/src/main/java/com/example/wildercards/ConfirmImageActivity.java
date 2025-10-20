package com.example.wildercards;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

public class ConfirmImageActivity extends BaseActivity {

    private static final String TAG = "ConfirmImageActivity";
    private static final String INATURALIST_API_URL = "https://api.inaturalist.org/v1/computervision/score_image";

    private ImageView confirmImageView;
    private ImageView ivSelectedImage;
    private Button btnConfirm;
    private Uri imageUri;
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
        }

//        String uriString = getIntent().getStringExtra("imageUri");
//        if (uriString != null) {
//            imageUri = Uri.parse(uriString);
//            ivSelectedImage.setImageURI(imageUri);
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageBytes = stream.toByteArray();

        String boundary = "Boundary-" + UUID.randomUUID().toString();

        RequestBody requestBody = new MultipartBody.Builder(boundary)
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "upload.jpg",
                        RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(INATURALIST_API_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "iNaturalist API call failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(ConfirmImageActivity.this, getString(R.string.api_call_failed),
                            Toast.LENGTH_LONG).show();
                    btnConfirm.setEnabled(true);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                    runOnUiThread(() -> {
                        Toast.makeText(ConfirmImageActivity.this, getString(R.string.api_error, response.code()),
                                Toast.LENGTH_LONG).show();
                        btnConfirm.setEnabled(true);
                    });
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData);
                    JSONArray results = jsonObject.getJSONArray("results");

                    if (results.length() > 0) {
                        JSONObject topResult = results.getJSONObject(0);
                        String animalName = topResult.getJSONObject("taxon").getString("name");
                        double score = topResult.getDouble("vision_score");

                        Log.d(TAG, "Animal Name: " + animalName + ", Score: " + score);

                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmImageActivity.this, getString(R.string.identified, animalName), Toast.LENGTH_LONG).show();

                            // change to navigate to result or "card" page
                            btnConfirm.setEnabled(true);
                        });
                    } else {
                        Log.d(TAG, "No results found");
                        runOnUiThread(() -> {
                            Toast.makeText(ConfirmImageActivity.this, getString(R.string.could_not_identify), Toast.LENGTH_LONG).show();
                            btnConfirm.setEnabled(true);
                        });
                    }
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