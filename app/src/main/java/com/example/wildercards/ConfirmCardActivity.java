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

    private String currentAnimalName = "Tiger";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_card);

        ivResult = findViewById(R.id.ivResult);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        setupRetryButton();

        // Generate initial image
        generateImage();

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

}