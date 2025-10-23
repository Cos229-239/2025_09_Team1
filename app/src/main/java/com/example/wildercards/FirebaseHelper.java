package com.example.wildercards;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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
     * Initialize user profile document with default values
     * Creates /users/{userId} document if it doesn't exist
     */
    public void initializeUserProfile(InitCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Cannot initialize profile - user not authenticated");
            if (callback != null) callback.onFailure("USER_NOT_AUTHENTICATED");
            return;
        }

        DocumentReference userRef = db.collection("users").document(userId);

        // Check if user document exists
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Log.d(TAG, "User profile already exists");
                if (callback != null) callback.onSuccess();
            } else {
                // Create new user profile
                Map<String, Object> userProfile = new HashMap<>();
                userProfile.put("userId", userId);
                userProfile.put("totalCoins", 0);
                userProfile.put("cardsCollected", 0);
                userProfile.put("joinedDate", System.currentTimeMillis());

                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null && currentUser.getEmail() != null) {
                    userProfile.put("email", currentUser.getEmail());
                }

                userRef.set(userProfile)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "User profile created successfully");
                            if (callback != null) callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to create user profile", e);
                            if (callback != null) callback.onFailure(e.getMessage());
                        });
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to check user profile", e);
            if (callback != null) callback.onFailure(e.getMessage());
        });
    }

    /**
     * Get user's total WilderCoins
     */
    public void getUserCoins(CoinsCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Cannot get coins - user not authenticated");
            if (callback != null) callback.onFailure("USER_NOT_AUTHENTICATED");
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long coins = documentSnapshot.getLong("totalCoins");
                        int totalCoins = coins != null ? coins.intValue() : 0;
                        Log.d(TAG, "User has " + totalCoins + " WilderCoins");
                        if (callback != null) callback.onSuccess(totalCoins);
                    } else {
                        // User document doesn't exist, initialize it
                        Log.d(TAG, "User profile doesn't exist, initializing...");
                        initializeUserProfile(new InitCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) callback.onSuccess(0);
                            }

                            @Override
                            public void onFailure(String error) {
                                if (callback != null) callback.onFailure(error);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user coins", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get user statistics (coins and cards count)
     */
    public void getUserStats(StatsCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Cannot get stats - user not authenticated");
            if (callback != null) callback.onFailure("USER_NOT_AUTHENTICATED");
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long coins = documentSnapshot.getLong("totalCoins");
                        Long cards = documentSnapshot.getLong("cardsCollected");

                        int totalCoins = coins != null ? coins.intValue() : 0;
                        int cardsCollected = cards != null ? cards.intValue() : 0;

                        Log.d(TAG, "User stats - Coins: " + totalCoins + ", Cards: " + cardsCollected);
                        if (callback != null) callback.onSuccess(totalCoins, cardsCollected);
                    } else {
                        // Initialize profile if doesn't exist
                        initializeUserProfile(new InitCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) callback.onSuccess(0, 0);
                            }

                            @Override
                            public void onFailure(String error) {
                                if (callback != null) callback.onFailure(error);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user stats", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Update user's total coins (atomic increment)
     * @param coinsToAdd Number of coins to add (can be negative to subtract)
     */
    private void updateUserCoins(int coinsToAdd, UpdateCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Cannot update coins - user not authenticated");
            if (callback != null) callback.onFailure("USER_NOT_AUTHENTICATED");
            return;
        }

        DocumentReference userRef = db.collection("users").document(userId);

        // Use FieldValue.increment for atomic operation
        userRef.update(
                "totalCoins", FieldValue.increment(coinsToAdd),
                "cardsCollected", FieldValue.increment(1)
        ).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Successfully added " + coinsToAdd + " WilderCoins");
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to update coins, trying to initialize profile", e);

            // If update fails, user document might not exist - initialize it
            initializeUserProfile(new InitCallback() {
                @Override
                public void onSuccess() {
                    // Try update again after initialization
                    userRef.update(
                            "totalCoins", FieldValue.increment(coinsToAdd),
                            "cardsCollected", FieldValue.increment(1)
                    ).addOnSuccessListener(aVoid2 -> {
                        Log.d(TAG, "Successfully added " + coinsToAdd + " WilderCoins after init");
                        if (callback != null) callback.onSuccess();
                    }).addOnFailureListener(e2 -> {
                        Log.e(TAG, "Failed to update coins after init", e2);
                        if (callback != null) callback.onFailure(e2.getMessage());
                    });
                }

                @Override
                public void onFailure(String error) {
                    if (callback != null) callback.onFailure(error);
                }
            });
        });
    }

    /**
     * Save animal card to user-specific Firestore subcollection WITH COIN REWARD
     * Path: /users/{userId}/animal_cards/{cardId}
     * Also updates user's totalCoins and cardsCollected
     */
    public void saveAnimalCard(Context context,
                               String animalName,
                               String description,
                               String imageUrl,
                               String sciName,
                               String habitat,
                               String conservation,
                               SaveCallback callback) {

        Log.d(TAG, "=== Starting to save card with WilderCoins ===");

        // Check if user is authenticated
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User not authenticated!");
            Toast.makeText(context, "Please login to save cards", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onFailure("USER_NOT_AUTHENTICATED", 0);
            return;
        }

        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Animal: " + animalName);
        Log.d(TAG, "Conservation Status: " + conservation);

        // Validate inputs
        if (animalName == null || animalName.isEmpty()) {
            Toast.makeText(context, "Animal name is required!", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onFailure("Animal name is required", 0);
            return;
        }

        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(context, "No image URL available!", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onFailure("No image URL", 0);
            return;
        }

        // Calculate WilderCoins earned based on conservation status
        int coinsEarned = ConservationStatusMapper.getCoinsForFullText(conservation);
        String statusAbbrev = ConservationStatusMapper.mapFullTextToAbbreviation(conservation);
        String rarityName = ConservationStatusMapper.getRarityName(statusAbbrev);

        Log.d(TAG, "WilderCoins to earn: " + coinsEarned + " (Rarity: " + rarityName + ")");

        // Create card data
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("animalName", animalName);
        cardData.put("description", description != null ? description : "");
        cardData.put("imageUrl", imageUrl);
        cardData.put("sciName", sciName != null ? sciName : "");
        cardData.put("habitat", habitat != null ? habitat : "");
        cardData.put("conservation", conservation != null ? conservation : "");
        cardData.put("conservationAbbrev", statusAbbrev);
        cardData.put("coinsEarned", coinsEarned);
        cardData.put("rarityName", rarityName);
        cardData.put("timestamp", System.currentTimeMillis());
        cardData.put("userId", userId);

        Log.d(TAG, "Saving to user-specific Firestore path...");

        // Save card to user-specific subcollection
        db.collection("users")
                .document(userId)
                .collection("animal_cards")
                .add(cardData)
                .addOnSuccessListener(documentReference -> {
                    String cardId = documentReference.getId();
                    Log.d(TAG, "Card saved successfully with ID: " + cardId);

                    // Now update user's total coins
                    updateUserCoins(coinsEarned, new UpdateCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "User coins updated successfully");

                            // Show success message with coins earned
                            String message = ConservationStatusMapper.getEarnedMessage(conservation);
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show();

                            if (callback != null) callback.onSuccess(cardId, coinsEarned);
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "Card saved but failed to update coins: " + error);
                            // Card is saved but coins update failed
                            Toast.makeText(context, "Card saved but coin update failed", Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onSuccess(cardId, coinsEarned);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save to Firestore", e);
                    Toast.makeText(context, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (callback != null) callback.onFailure(e.getMessage(), 0);
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
     * Callback interface for initialization operations
     */
    public interface InitCallback {
        void onSuccess();
        void onFailure(String error);
    }

    /**
     * Callback interface for save operations (now includes coins earned)
     */
    public interface SaveCallback {
        void onSuccess(String cardId, int coinsEarned);
        void onFailure(String error, int coinsEarned);
    }

    /**
     * Callback interface for fetch operations
     */
    public interface FetchCallback {
        void onSuccess(List<AnimalCard> cards);
        void onFailure(String error);
    }

    /**
     * Callback interface for coin operations
     */
    public interface CoinsCallback {
        void onSuccess(int totalCoins);
        void onFailure(String error);
    }

    /**
     * Callback interface for stats operations
     */
    public interface StatsCallback {
        void onSuccess(int totalCoins, int cardsCollected);
        void onFailure(String error);
    }

    /**
     * Callback interface for update operations
     */
    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String error);
    }
}