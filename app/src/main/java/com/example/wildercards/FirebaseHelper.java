package com.example.wildercards;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

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
                               String imageUrl,
                               String sciName,
                               String habitat,
                               String conservation,
                               SaveCallback callback) {

        Log.d(TAG, "=== Starting to save card ===");
        Log.d(TAG, "Animal: " + animalName);
        Log.d(TAG, "Description: " + description);
        Log.d(TAG, "Image URL: " + imageUrl);
        Log.d(TAG, "Scientific Name: " + sciName);
        Log.d(TAG, "Habitat: " + habitat);
        Log.d(TAG, "Conservation: " + conservation);


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
        cardData.put("sciName", sciName != null ? sciName : "");
        cardData.put("habitat", habitat != null ? habitat : "");
        cardData.put("conservation", conservation != null ? conservation : "");
        cardData.put("timestamp", System.currentTimeMillis());

        Log.d(TAG, "Saving to Firestore...");

        // Save to Firestore collection "animal_cards"
        db.collection("animal_cards")
                .add(cardData)
                .addOnSuccessListener(documentReference -> {
                    String cardId = documentReference.getId();
                    Log.d(TAG, "Card saved successfully with ID: " + cardId);
                    Toast.makeText(context, "Card saved to collection! ✓", Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onSuccess(cardId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save to Firestore", e);
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

    public interface FetchCallback {
        void onSuccess(List<AnimalCard> cards);
        void onFailure(String error);
    }

    public void fetchAllAnimalCards(FetchCallback callback) {
        Log.d(TAG, "=== Fetching all animal cards from Firestore ===");

        db.collection("animal_cards")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Newest first
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AnimalCard> cards = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            // Convert Firestore document to AnimalCard object
                            AnimalCard card = document.toObject(AnimalCard.class);
                            card.setCardId(document.getId()); // Set the document ID
                            cards.add(card);

                            Log.d(TAG, "Loaded card: " + card.getAnimalName());
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing card document: " + document.getId(), e);
                        }
                    }

                    Log.d(TAG, "Successfully fetched " + cards.size() + " cards");
                    if (callback != null) {
                        callback.onSuccess(cards);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch cards from Firestore", e);
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }
}