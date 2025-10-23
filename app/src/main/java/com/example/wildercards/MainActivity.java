package com.example.wildercards;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    // UI Components
    private ImageView topCardImage;
    private TextView topCardPlaceholder;
    private LinearLayout collectionsContainer;
    private TextView collectionsTitle;

    private LinearLayout animalsContainer;

    // Coin display components
    private ImageView ivCoinIcon;
    private TextView tvTotalCoins;

    // Firebase Helper
    private FirebaseHelper firebaseHelper;

    private RecyclerView recyclerView;
    private AnimalAdapter animalAdapter;
    private List<TopCards> animalList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "========== MainActivity onCreate() ==========");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.animalsRecyclerView);
        Log.d(TAG, "RecyclerView initialized");

        // Set horizontal layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
        );
        recyclerView.setLayoutManager(layoutManager);

        // Load animals from JSON
        animalList = loadAnimalsFromJson();

        // Set up adapter
        animalAdapter = new AnimalAdapter(this, animalList);
        recyclerView.setAdapter(animalAdapter);

        TextView collectionsTv = findViewById(R.id.collections);
        collectionsTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CollectionsActivity.class);
                startActivity(intent);
            }
        });

        topCardImage = findViewById(R.id.topCardImage);
        topCardPlaceholder = findViewById(R.id.topCardPlaceholder);
        collectionsContainer = findViewById(R.id.collectionsContainer);
        collectionsTitle = findViewById(R.id.collections);

        // Initialize coin display views
        ivCoinIcon = findViewById(R.id.ivCoinIcon);
        tvTotalCoins = findViewById(R.id.tvTotalCoins);

        // Check if coin views exist
        if (ivCoinIcon == null) {
            Log.e(TAG, "ERROR: ivCoinIcon is NULL! Did you add coin layout to activity_main.xml?");
        }
        if (tvTotalCoins == null) {
            Log.e(TAG, "ERROR: tvTotalCoins is NULL! Did you add coin layout to activity_main.xml?");
        } else {
            Log.d(TAG, "Coin views initialized successfully");
        }

        // Start subtle pulse animation on coin icon
        if (ivCoinIcon != null) {
            startCoinPulseAnimation();
        }

        // Initialize Firebase
        firebaseHelper = new FirebaseHelper();

        // Start subtle pulse animation on coin icon
        if (ivCoinIcon != null) {
            startCoinPulseAnimation();
        }

        // Load user's coins
        loadUserCoins();

        // Load cards from Firebase (only if user is authenticated)
        loadHomeData();

        // animalsContainer = findViewById(R.id.animalsContainer);



    }

    /**
     * Load user's WilderCoins and display in UI
     */
    private void loadUserCoins() {
        Log.d(TAG, "=== loadUserCoins() called ===");

        // Check if TextView exists
        if (tvTotalCoins == null) {
            Log.e(TAG, "ERROR: tvTotalCoins is NULL! Cannot display coins.");
            Log.e(TAG, "Make sure you added the coin layout to activity_main.xml");
            return;
        }

        // Check if user is authenticated
        if (!firebaseHelper.isUserAuthenticated()) {
            Log.d(TAG, "User not authenticated, showing 0 coins");
            tvTotalCoins.setText("0");
            return;
        }

        Log.d(TAG, "User authenticated, fetching coins from Firebase...");

        // Fetch user's coins from Firebase
        firebaseHelper.getUserCoins(new FirebaseHelper.CoinsCallback() {
            @Override
            public void onSuccess(int totalCoins) {
                if (isFinishing() || isDestroyed()) {
                    Log.w(TAG, "Activity finishing, skipping coin update");
                    return;
                }

                Log.d(TAG, "✅ SUCCESS: User has " + totalCoins + " WilderCoins");

                if (tvTotalCoins != null) {
                    // Get current displayed value
                    String currentText = tvTotalCoins.getText().toString();
                    int currentCoins = 0;
                    try {
                        currentCoins = Integer.parseInt(currentText);
                    } catch (NumberFormatException e) {
                        currentCoins = 0;
                    }

                    Log.d(TAG, "Animating coins from " + currentCoins + " to " + totalCoins);
                    // Animate the coin count
                    animateCoinCount(currentCoins, totalCoins);
                } else {
                    Log.e(TAG, "ERROR: tvTotalCoins became null during callback!");
                }
            }

            @Override
            public void onFailure(String error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                Log.e(TAG, "❌ FAILED to load coins: " + error);

                if (tvTotalCoins != null) {
                    tvTotalCoins.setText("0");
                } else {
                    Log.e(TAG, "ERROR: tvTotalCoins is null, cannot show error state");
                }
            }
        });
    }

    /**
     * Animate coin count from old value to new value
     */
    private void animateCoinCount(int from, int to) {
        if (tvTotalCoins == null) return;

        // Animate the coin icon bounce
        if (ivCoinIcon != null && from != to) {
            animateCoinIconBounce();
        }

        // Simple animation: count up from old to new
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(from, to);
        animator.setDuration(800); // 800ms animation
        animator.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                if (tvTotalCoins != null) {
                    tvTotalCoins.setText(String.valueOf(animation.getAnimatedValue()));
                }
            }
        });
        animator.start();
    }

    /**
     * Bounce animation for coin icon
     */
    private void animateCoinIconBounce() {
        if (ivCoinIcon == null) return;

        // Scale up
        android.animation.ObjectAnimator scaleUpX = android.animation.ObjectAnimator.ofFloat(ivCoinIcon, "scaleX", 1f, 1.3f);
        android.animation.ObjectAnimator scaleUpY = android.animation.ObjectAnimator.ofFloat(ivCoinIcon, "scaleY", 1f, 1.3f);

        scaleUpX.setDuration(200);
        scaleUpY.setDuration(200);

        scaleUpX.setRepeatCount(1);
        scaleUpY.setRepeatCount(1);

        scaleUpX.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        scaleUpY.setRepeatMode(android.animation.ValueAnimator.REVERSE);

        scaleUpX.start();
        scaleUpY.start();

        // Rotate animation
        android.animation.ObjectAnimator rotate = android.animation.ObjectAnimator.ofFloat(ivCoinIcon, "rotation", 0f, 360f);
        rotate.setDuration(400);
        rotate.start();

        Log.d(TAG, "Coin icon bounce animation started");
    }

    /**
     * Start subtle pulse animation on coin icon (continuous)
     */
    private void startCoinPulseAnimation() {
        if (ivCoinIcon == null) return;

        // Subtle scale pulse
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(ivCoinIcon, "scaleX", 1f, 1.1f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(ivCoinIcon, "scaleY", 1f, 1.1f);

        scaleX.setDuration(1500);
        scaleY.setDuration(1500);

        scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);

        scaleX.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        scaleY.setRepeatMode(android.animation.ValueAnimator.REVERSE);

        scaleX.start();
        scaleY.start();

        Log.d(TAG, "Coin pulse animation started");
    }
    private void loadHomeData() {
        Log.d(TAG, "Loading home data from Firebase...");

        // Check if user is authenticated
        if (!firebaseHelper.isUserAuthenticated()) {
            Log.d(TAG, "User not authenticated, showing placeholder");
            topCardPlaceholder.setVisibility(View.VISIBLE);
            topCardPlaceholder.setText("Login to view your cards");
            return;
        }

        firebaseHelper.fetchUserAnimalCards(new FirebaseHelper.FetchCallback() {
            @Override
            public void onSuccess(List<AnimalCard> cards) {
                if (isFinishing() || isDestroyed()) {
                    Log.d(TAG, "Activity destroyed, skipping UI update");
                    return;
                }
                Log.d(TAG, "Successfully loaded " + cards.size() + " cards for home");

                if (cards.isEmpty()) {
                    // Show placeholder state
                    topCardPlaceholder.setVisibility(View.VISIBLE);
                    topCardPlaceholder.setText("No cards yet!\nCreate your first card");
                    return;
                }

                // FIX: Display the LAST card in the top card section (newest card)
                displayTopCard(cards.get(0)); // First card is newest (ordered by timestamp DESC)

                // FIX: Display all cards in horizontal scroll
                displayCollectionCards(cards);
            }

            @Override
            public void onFailure(String error) {
                if (isFinishing() || isDestroyed()) {
                    Log.d(TAG, "Activity destroyed, skipping error toast");
                    return;
                }

                Log.e(TAG, "Failed to load home data: " + error);

                // Handle authentication error
                if ("USER_NOT_AUTHENTICATED".equals(error)) {
                    topCardPlaceholder.setVisibility(View.VISIBLE);
                    topCardPlaceholder.setText("Login to view your cards");
                } else {
                    Toast.makeText(MainActivity.this,
                            "Failed to load cards: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Display the top/featured card (last saved card)
     */
    private void displayTopCard(AnimalCard card) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (card.getImageUrl() != null && !card.getImageUrl().isEmpty()) {
            // Hide placeholder text
            topCardPlaceholder.setVisibility(View.GONE);

            // Load image with Glide
            Glide.with(this)
                    .load(card.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(topCardImage);

            Log.d(TAG, "Top card loaded: " + card.getAnimalName());

            // FIX: Optional - Add click listener to top card
            topCardImage.setOnClickListener(v -> {
                Toast.makeText(this, "Latest: " + card.getAnimalName(), Toast.LENGTH_SHORT).show();
                // Could open detail view here
            });
        }
    }

    /**
     * Display collection cards in horizontal scroll
     * Shows up to 10 most recent cards
     */
    private void displayCollectionCards(List<AnimalCard> cards) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        // Clear existing views
        collectionsContainer.removeAllViews();

        // Limit to first 10 cards for performance
        int maxCards = Math.min(cards.size(), 10);

        for (int i = 0; i < maxCards; i++) {
            AnimalCard card = cards.get(i);
            addCollectionCard(card);
        }

        Log.d(TAG, "Displayed " + maxCards + " collection cards");
    }

    /**
     * Add a single collection card to the horizontal scroll
     */
    private void addCollectionCard(AnimalCard card) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        // Inflate the item layout
        View cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_home_collection_card, collectionsContainer, false);

        // Get views
        ImageView cardImage = cardView.findViewById(R.id.collectionCardImage);
        TextView cardBrand = cardView.findViewById(R.id.collectionCardBrand);
        TextView cardName = cardView.findViewById(R.id.collectionCardName);
        TextView rarityBadge = cardView.findViewById(R.id.rarityBadge); // Add this to your layout

        // Set data
        cardName.setText(card.getAnimalName());

        // Set brand/scientific name
        String sciName = card.getSciName();
        if (sciName != null && !sciName.isEmpty() && !sciName.equals("Unknown")) {
            cardBrand.setText(sciName);
        } else {
            cardBrand.setText("Wildlife");
        }

        // Apply rarity badge if TextView exists
        if (rarityBadge != null && card.getConservation() != null) {
            RarityBadgeHelper.applyRarityBadge(rarityBadge, card.getConservation());
        }

        // Load image
        if (card.getImageUrl() != null && !card.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(card.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background) // FIX: Use your placeholder
                    .into(cardImage);
        }

        // FIX: Add click listener
        cardView.setOnClickListener(v -> {
            Toast.makeText(this, card.getAnimalName(), Toast.LENGTH_SHORT).show();
            // Could navigate to detail view or full collection
            Intent intent = new Intent(MainActivity.this, CollectionsActivity.class);
            startActivity(intent);
        });

        // Add to container
        collectionsContainer.addView(cardView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called - refreshing coins");
        // Reload coins when returning to home screen (in case new card was saved)
        loadUserCoins();
        // FIX: Reload data when returning to home screen
        // loadHomeData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called - refreshing coins");
        // Also refresh on start
        loadUserCoins();
    }

    private List<TopCards> loadAnimalsFromJson() {
        List<TopCards> animals = new ArrayList<>();

        try {
            // Read JSON file from assets
            InputStream inputStream = getAssets().open("topCards.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            Log.d("AnimalDebug", "JSON String: " + jsonString);

            // Parse JSON using Gson
            Gson gson = new Gson();
            AnimalResponse response = gson.fromJson(jsonString, AnimalResponse.class);

            if (response != null && response.getAnimals() != null) {
                animals = response.getAnimals();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("AnimalDebug", "Error loading JSON: " + e.getMessage());
        }

        return animals;
    }


}