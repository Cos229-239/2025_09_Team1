package com.example.wildercards;

import android.os.Bundle;
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

import com.example.wildercards.ImageGenerator;


public class ConfirmCardActivity extends BaseActivity {
    private ImageView ivResult;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvAnimalName;
    private TextView tvDescription;
    private Button btnSave;


    private String currentAnimalName = "Northern cardinal";
    private String currentDescription = "A beautiful red bird found in North America";

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
        btnSave = findViewById(R.id.btn_save);
        setupRetryButton();

        // Generate initial image
        generateImage();

        firebaseHelper = new FirebaseHelper();

        // Set animal name and description
        tvAnimalName.setText(currentAnimalName);
        tvDescription.setText(currentDescription);

        // Generate initial image
        ImageGenerator.generateAnimalImage(this, currentAnimalName, ivResult, progressBar, tvStatus);


        // Save button
        btnSave.setOnClickListener(v -> {
            saveCardToFirebase();
        });


        // Example
        //String animalName = "Northern cardinal";
//        ImageGenerator.generateAnimalImage(this, animalName, ivResult, progressBar, tvStatus);

    }
    private void generateImage() {
        currentAnimalName = "Tiger";
        ImageGenerator.generateAnimalImage(this, currentAnimalName, ivResult, progressBar, tvStatus);
    }

    // Set up the retry button
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
                imageUrl,  // ✅ Pass the URL directly
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