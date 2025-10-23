package com.example.wildercards;

import static android.content.ContentValues.TAG;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;

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

    private String currentAnimalName = "Tiger";
    private String currentDescription = "A beautiful red bird found in North America";

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_card);

        // Initialize views
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

        // Null checks
        if (tvAnimalName == null) Log.e(TAG, "tvAnimalName is null!");
        if (scientificNameTextView == null) Log.e(TAG, "scientificNameTextView is null!");
        if (tvDescription == null) Log.e(TAG, "tvDescription is null!");
        if (habitatTextView == null) Log.e(TAG, "habitatTextView is null!");
        if (conservationTextView == null) Log.e(TAG, "conservationTextView is null!");
        if (animalImageView == null) Log.e(TAG, "animalImageView is null!");

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper();

        // Check if user is authenticated when activity starts
        checkUserAuthentication();

        // Set up buttons
        setupRetryButton();

        // Set animal name and description
        tvAnimalName.setText(currentAnimalName);
        tvDescription.setText(currentDescription);

        // Generate initial image
        generateImage();

        // Save button - with authentication check
        btnSave.setOnClickListener(v -> {
            saveCardToFirebase();
        });

        // Fetch Wikipedia data in background
        new Thread(() -> {
            AnimalInfo info = WikipediaFetcher.fetchAnimalInfo("Northern cardinal");

            runOnUiThread(() -> {
                if (info != null) {
                    if (tvAnimalName != null) tvAnimalName.setText(info.getName());
                    if (scientificNameTextView != null) scientificNameTextView.setText(info.getScientificName());
                    if (tvDescription != null) tvDescription.setText(info.getDescription());
                    if (habitatTextView != null) habitatTextView.setText(info.getHabitat());
                    if (conservationTextView != null && info.getConservationStatus() != null) {
                        conservationTextView.setText("Status: " + info.getConservationStatus());
                    }
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
    }

    /**
     * Check if user is authenticated, redirect to login if not
     */
    private void checkUserAuthentication() {
        if (!firebaseHelper.isUserAuthenticated()) {
            Log.w(TAG, "User not authenticated, but allowing them to view. Save will prompt login.");
            // Optionally update UI to show login prompt
            // For now, we'll just check on save
        }
    }

    /**
     * Generate the animal image
     */
    private void generateImage() {
        ImageGenerator.generateAnimalImage(this, currentAnimalName, ivResult, progressBar, tvStatus);
    }

    /**
     * Set up the retry button
     */
    private void setupRetryButton() {
        Button btnTryAgain = findViewById(R.id.btn_try_again);
        btnTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentAnimalName.isEmpty()) {
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

    /**
     * Save card to Firebase (user-specific) with WilderCoins reward
     */
    private void saveCardToFirebase() {
        // First check if user is authenticated
        if (!firebaseHelper.isUserAuthenticated()) {
            Log.w(TAG, "User not authenticated. Redirecting to login...");
            Toast.makeText(this, "Please login to save cards", Toast.LENGTH_LONG).show();

            // Redirect to LoginActivity
            Intent loginIntent = new Intent(ConfirmCardActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            return;
        }

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
        String conservationStatus = conservationTextView.getText().toString();

        // Clean conservation status (remove "Status: " prefix if present)
        if (conservationStatus.startsWith("Status: ")) {
            conservationStatus = conservationStatus.substring(8).trim();
        }

        Log.d(TAG, "Saving card with conservation status: " + conservationStatus);

        // Save to Firebase (user-specific path with coins)
        firebaseHelper.saveAnimalCard(
                this,
                animalName,
                description,
                imageUrl,
                scientificNameTextView.getText().toString(),
                habitatTextView.getText().toString(),
                conservationStatus,
                new FirebaseHelper.SaveCallback() {
                    @Override
                    public void onSuccess(String cardId, int coinsEarned) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Collection");

                        // The toast with coins is already shown by FirebaseHelper
                        Log.d(TAG, "Card saved successfully with ID: " + cardId);
                        Log.d(TAG, "WilderCoins earned: " + coinsEarned);

                        // Optional: Add coin animation here in future
                        // showCoinAnimation(coinsEarned);
                    }

                    @Override
                    public void onFailure(String error, int coinsEarned) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Collection");

                        // Handle authentication error specifically
                        if ("USER_NOT_AUTHENTICATED".equals(error)) {
                            Toast.makeText(ConfirmCardActivity.this, "Please login to save cards", Toast.LENGTH_LONG).show();

                            // Redirect to login
                            Intent loginIntent = new Intent(ConfirmCardActivity.this, LoginActivity.class);
                            startActivity(loginIntent);
                        } else {
                            Toast.makeText(ConfirmCardActivity.this, "Save failed: " + error, Toast.LENGTH_SHORT).show();
                        }

                        Log.e(TAG, "Failed to save card: " + error);
                    }
                }
        );
    }
}