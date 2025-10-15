package com.example.wildercards;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ImageGenerator {

    public static void generateAnimalImage(Context context,
                                           String animalName,
                                           ImageView imageView,
                                           ProgressBar progressBar,
                                           TextView statusText) {
        try {
            // Sanitize and normalize the animal name
            String sanitizedName = sanitizeAnimalName(animalName);

            // Build more specific prompt
            String hiddenPrompt = "A " + sanitizedName + " animal, Pokémon trading card style illustration, " +
                    "professional digital art, centered portrait, vibrant colors, holographic border effect, " +
                    "fantasy creature design, detailed features, clean background, high quality";

            // URL-encode for Pollinations
            String encodedPrompt = URLEncoder.encode(hiddenPrompt, "UTF-8");

            // Add timestamp or random seed to force new generation each time
            long timestamp = System.currentTimeMillis();
            String seed = String.valueOf((int)(Math.random() * 1000000));

            // Pollinations endpoint with seed parameter to generate different images
            String url = "https://image.pollinations.ai/prompt/" + encodedPrompt +
                    "?width=512&height=512&model=flux&nologo=true&enhance=true&seed=" + seed;

            // Update UI
            progressBar.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            statusText.setText("Generating " + animalName + " in Pokémon style...");

            // Load with Glide with better error handling
            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache
                    .skipMemoryCache(true) // Skip memory cache
                    .timeout(30000) // 30 second timeout
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
                            imageView.setVisibility(View.VISIBLE);
                            statusText.setText("Failed to generate. Tap to retry.");
                            Toast.makeText(context, "Could not generate " + animalName + ". Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            statusText.setText("Encoding error occurred.");
            Toast.makeText(context, "Error encoding animal name", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sanitizes animal names to improve API consistency
     */
    private static String sanitizeAnimalName(String name) {
        if (name == null || name.isEmpty()) {
            return "animal";
        }

        // Remove extra whitespace and trim
        String sanitized = name.trim().replaceAll("\\s+", " ");

        // Convert to lowercase for consistency
        sanitized = sanitized.toLowerCase();

        // Remove any special characters that might cause issues
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9\\s-]", "");

        // Handle common naming issues
        if (sanitized.length() < 2) {
            sanitized = "animal";
        }

        return sanitized;
    }
}