package com.example.wildercards;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";
    private FirebaseFirestore db;

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Firebase Firestore initialized");
    }

    /**
     * Save animal card to Firestore (saves the Pollinations.ai URL directly)
     */
    public void saveAnimalCard(Context context,
                               String animalName,
                               String description,
                               String imageUrl,  // ✅ Now we pass the URL directly
                               SaveCallback callback) {

        Log.d(TAG, "=== Starting to save card ===");
        Log.d(TAG, "Animal: " + animalName);
        Log.d(TAG, "Description: " + description);
        Log.d(TAG, "Image URL: " + imageUrl);

        // Validate inputs
        if (animalName == null || animalName.isEmpty()) {
            Toast.makeText(context, "Animal name is required!", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onFailure("Animal name is required");
            return;
        }

        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(context, "No image URL available!", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onFailure("No image URL");
            return;
        }

        // Create card data
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("animalName", animalName);
        cardData.put("description", description != null ? description : "");
        cardData.put("imageUrl", imageUrl);
        cardData.put("timestamp", System.currentTimeMillis());

        Log.d(TAG, "Saving to Firestore...");

        // Save to Firestore collection "animal_cards"
        db.collection("animal_cards")
                .add(cardData)
                .addOnSuccessListener(documentReference -> {
                    String cardId = documentReference.getId();
                    Log.d(TAG, "✅ Card saved successfully with ID: " + cardId);
                    Toast.makeText(context, "Card saved to collection! ✓", Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onSuccess(cardId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save to Firestore", e);
                    Toast.makeText(context, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Callback interface for save operations
     */
    public interface SaveCallback {
        void onSuccess(String cardId);
        void onFailure(String error);
    }
}