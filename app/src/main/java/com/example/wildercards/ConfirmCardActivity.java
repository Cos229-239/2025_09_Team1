package com.example.wildercards;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.wildercards.ImageGenerator;


public class ConfirmCardActivity extends BaseActivity {
    private ImageView ivResult;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvAnimalName;
    private TextView tvDescription;
    private Button btnSave;


    private TextView scientificNameTextView;
    private TextView habitatTextView;
    private TextView conservationTextView;
    private ImageView animalImageView;


    private String currentAnimalName ;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_card);

        ivResult = findViewById(R.id.ivResult);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvAnimalName = findViewById(R.id.tvResultLabel);
        tvDescription = findViewById(R.id.tvDescriptionLabel);
        scientificNameTextView = findViewById(R.id.scientificNameTextView);
        habitatTextView = findViewById(R.id.habitatTextView);
        conservationTextView = findViewById(R.id.conservationTextView);

        animalImageView = findViewById(R.id.animalViewWiki);

        btnSave = findViewById(R.id.btn_save);

        if (tvAnimalName == null) Log.e(TAG, "tvAnimalName is null!");
        if (scientificNameTextView == null) Log.e(TAG, "scientificNameTextView is null!");
        if (tvDescription == null) Log.e(TAG, "tvDescription is null!");
        if (habitatTextView == null) Log.e(TAG, "habitatTextView is null!");
        if (conservationTextView == null) Log.e(TAG, "conservationTextView is null!");
        if (animalImageView == null) Log.e(TAG, "animalImageView is null!");

        currentAnimalName = getIntent().getStringExtra("animal_name");
        if (currentAnimalName == null || currentAnimalName.isEmpty()) {
            Toast.makeText(this, "Animal name not provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRetryButton();

        // Generate initial image


        firebaseHelper = new FirebaseHelper();

        // Set animal name and description
        tvAnimalName.setText(currentAnimalName);

        // Generate initial image
        // ImageGenerator.generateAnimalImage(this, currentAnimalName, ivResult, progressBar, tvStatus);
        generateImage();

        // Save button
        btnSave.setOnClickListener(v -> {
            saveCardToFirebase();
        });


//        new Thread(() -> {
//            AnimalInfo info = WikipediaFetcher.fetchAnimalInfo("Northern cardinal");
//
//            runOnUiThread(() -> {
//                if (info != null) {
//                    if (tvAnimalName != null) tvAnimalName.setText(info.getName());
//                    if (scientificNameTextView != null) scientificNameTextView.setText(info.getScientificName());
//                    if (tvDescription != null) tvDescription.setText(info.getDescription());
//                    if (habitatTextView != null) habitatTextView.setText(info.getHabitat());
//                    if (conservationTextView != null) conservationTextView.setText(info.getConservationStatus());
//
//                    if (animalImageView != null && info.getImageUrl() != null && !info.getImageUrl().isEmpty()) {
//                        Glide.with(ConfirmCardActivity.this)
//                                .load(info.getImageUrl())
//                                .into(animalImageView);
//                    }
//                } else {
//                    Toast.makeText(ConfirmCardActivity.this, "No info found!", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }).start();

        new Thread(() -> {
            AnimalInfo info = WikipediaFetcher.fetchAnimalInfo(currentAnimalName);

            runOnUiThread(() -> {
                if (info != null) {
                    if (tvAnimalName != null) tvAnimalName.setText(info.getName());
                    if (scientificNameTextView != null) scientificNameTextView.setText(info.getScientificName());
                    if (tvDescription != null) tvDescription.setText(info.getDescription());
                    if (habitatTextView != null) habitatTextView.setText(info.getHabitat());
                    if (conservationTextView != null && info.getConservationStatus() != null) { conservationTextView.setText("Status: " + info.getConservationStatus());}
                    if (animalImageView != null && info.getImageUrl() != null && !info.getImageUrl().isEmpty()) {
                        Glide.with(ConfirmCardActivity.this)
                                .load(info.getImageUrl())
                                .into(animalImageView);
                    }
                } else {
                    Toast.makeText(ConfirmCardActivity.this, "Failed to fetch animal info", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();

        // Example
        //String animalName = "Northern cardinal";
//        ImageGenerator.generateAnimalImage(this, animalName, ivResult, progressBar, tvStatus);

    }
    private void generateImage() {
        ImageGenerator.generateAnimalImage(this, currentAnimalName, ivResult, progressBar, tvStatus);
    }

    // Set up the retry button
    private void setupRetryButton() {
        Button btnTryAgain = findViewById(R.id.btn_try_again);
        btnTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentAnimalName != null && !currentAnimalName.isEmpty()) {
                    // Regenerate the image with the same animal name
                    ImageGenerator.generateAnimalImage(
                            ConfirmCardActivity.this,
                            currentAnimalName,
                            ivResult,
                            progressBar,
                            tvStatus
                    );
                } else {
                    Toast.makeText(ConfirmCardActivity.this, "No animal to regenerate", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveCardToFirebase() {
        // Get the image URL from ImageGenerator
        String imageUrl = ImageGenerator.getLastGeneratedImageUrl();

        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Please generate an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button while saving
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Get current values
        String animalName = tvAnimalName.getText().toString();
        String description = tvDescription.getText().toString();

        // Save to Firebase (just the URL, no image upload needed!)
        firebaseHelper.saveAnimalCard(
                this,
                animalName,
                description,
                imageUrl,
                scientificNameTextView.getText().toString(),
                habitatTextView.getText().toString(),
                conservationTextView.getText().toString(),
                new FirebaseHelper.SaveCallback() {
                    @Override
                    public void onSuccess(String cardId) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Collection");
                        Toast.makeText(ConfirmCardActivity.this, "Card saved! ✓", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String error) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Collection");
                        Toast.makeText(ConfirmCardActivity.this, "Save failed: " + error, Toast.LENGTH_SHORT).show();
                    }

                }


        );
    }
}
