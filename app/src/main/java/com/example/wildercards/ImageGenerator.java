package com.example.wildercards;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ImageGenerator {

    public static void generateAnimalImage(Context context,
                                           String animalName,
                                           ImageView imageView,
                                           ProgressBar progressBar,
                                           TextView statusText) {
        try {
            // Build hidden prompt
            String hiddenPrompt = animalName + ", Pokémon card style, hand-drawn but still realistic portrait, shiny holographic effect, clean line art, vibrant colors, high detail, centered composition";
            // String prompt = animalName + " in the style of a detailed, vibrant Pokémon card, hand-drawn, dynamic lighting, fantasy art, holographic glow";

            // URL-encode for Pollinations
            String encodedPrompt = URLEncoder.encode(hiddenPrompt, "UTF-8");

            // Pollinations endpoint
            String url = "https://image.pollinations.ai/prompt/" + encodedPrompt;

            // Update UI
            progressBar.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            statusText.setText("Generating " + animalName + " in Pokémon style...");

            // Load with Glide
            Glide.with(context)
                    .load(url)
                    .into(new com.bumptech.glide.request.target.ImageViewTarget<android.graphics.drawable.Drawable>(imageView) {
                        @Override
                        protected void setResource(android.graphics.drawable.Drawable resource) {
                            progressBar.setVisibility(View.GONE);
                            imageView.setVisibility(View.VISIBLE);
                            imageView.setImageDrawable(resource);
                            statusText.setText("Generated successfully!");
                        }

                        @Override
                        public void onLoadFailed(android.graphics.drawable.Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            progressBar.setVisibility(View.GONE);
                            statusText.setText("Failed to generate image.");
                            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(context, "Encoding error", Toast.LENGTH_SHORT).show();
        }
    }
}
