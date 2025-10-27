package com.example.wildercards;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ImageGenerator {
    private static String lastGeneratedImageUrl = "";

    public interface ImageGenerationCallback {
        void onImageGenerated(String imageUrl, Drawable resource);
        void onImageGenerationFailed();
    }

    public static void generateAnimalImage(Context context,
                                           String animalName,
                                           ImageGenerationCallback callback) {
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
            String seed = String.valueOf((int)(Math.random() * 1000000));

            // Pollinations endpoint with seed parameter to generate different images
            String url = "https://image.pollinations.ai/prompt/" + encodedPrompt +
                    "?width=512&height=512&model=flux&nologo=true&enhance=true&seed=" + seed;

            lastGeneratedImageUrl = url;

            // Load with Glide with better error handling
            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache
                    .skipMemoryCache(true) // Skip memory cache
                    .timeout(30000) // 30 second timeout
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            if (callback != null) {
                                callback.onImageGenerated(lastGeneratedImageUrl, resource);
                            }
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            Log.e("ImageGenerator", "Failed to load image from: " + url);
                            Toast.makeText(context, "Could not generate " + animalName + ". Try again.", Toast.LENGTH_SHORT).show();
                            if (callback != null) {
                                callback.onImageGenerationFailed();
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Not used
                        }
                    });

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error encoding animal name", Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onImageGenerationFailed();
            }
        }
    }

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

    public static String getLastGeneratedImageUrl() {
        return lastGeneratedImageUrl;
    }

}