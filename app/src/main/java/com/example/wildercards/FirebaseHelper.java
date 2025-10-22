package com.example.wildercards;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        Log.d(TAG, "Firebase Firestore and Auth initialized");
    }

    /**
     * Get current authenticated user ID
     * @return userId or null if not authenticated
     */
    private String getCurrentUserId() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        return null;
    }

    /**
     * Check if user is authenticated
     * @return true if user is logged in
     */
    public boolean isUserAuthenticated() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * Save animal card to user-specific Firestore subcollection
     * Path: /users/{userId}/animal_cards/{cardId}
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

        // Check if user is authenticated
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not authenticated!");
            Toast.makeText(context, "Please login to save cards", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onFailure("USER_NOT_AUTHENTICATED");
            return;
        }

        Log.d(TAG, "User ID: " + userId);
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
        cardData.put("userId", userId); // Store userId for reference

        Log.d(TAG, "Saving to user-specific Firestore path...");

        // Save to user-specific subcollection: /users/{userId}/animal_cards
        db.collection("users")
                .document(userId)
                .collection("animal_cards")
                .add(cardData)
                .addOnSuccessListener(documentReference -> {
                    String cardId = documentReference.getId();
                    Log.d(TAG, "Card saved successfully with ID: " + cardId);
                    Toast.makeText(context, "Card saved to your collection! ✓", Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onSuccess(cardId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save to Firestore", e);
                    Toast.makeText(context, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Fetch only the current user's animal cards
     * Path: /users/{userId}/animal_cards
     */
    public void fetchUserAnimalCards(FetchCallback callback) {
        Log.d(TAG, "=== Fetching user's animal cards from Firestore ===");

        // Check if user is authenticated
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not authenticated!");
            if (callback != null) {
                callback.onFailure("USER_NOT_AUTHENTICATED");
            }
            return;
        }

        Log.d(TAG, "Fetching cards for user: " + userId);

        // Fetch from user-specific subcollection
        db.collection("users")
                .document(userId)
                .collection("animal_cards")
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

                    Log.d(TAG, "Successfully fetched " + cards.size() + " cards for user");
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

    /**
     * Callback interface for save operations
     */
    public interface SaveCallback {
        void onSuccess(String cardId);
        void onFailure(String error);
    }

    /**
     * Callback interface for fetch operations
     */
    public interface FetchCallback {
        void onSuccess(List<AnimalCard> cards);
        void onFailure(String error);
    }
}