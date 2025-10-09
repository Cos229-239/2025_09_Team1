package com.example.wildercards;

import android.os.Bundle;
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


public class ConfirmImageActivity extends BaseActivity {
    private ImageView ivResult;
    private ProgressBar progressBar;
    private TextView tvStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_image);

        ivResult = findViewById(R.id.ivResult);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        // Example usage:
        String animalName = "cat";  // or get from intent, etc.
        ImageGenerator.generateAnimalImage(this, animalName, ivResult, progressBar, tvStatus);
    }


}