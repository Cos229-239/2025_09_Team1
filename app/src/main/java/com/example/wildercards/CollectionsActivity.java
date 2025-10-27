package com.example.wildercards;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CollectionsActivity extends BaseActivity {

    private static final String TAG = "CollectionsActivity";

    // UI Components
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    // Data
    private AnimalCardAdapter adapter;
    private List<AnimalCard> animalCards;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collections);

        Log.d(TAG, "CollectionsActivity created");

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        // Initialize Firebase helper
        firebaseHelper = new FirebaseHelper();

        // Initialize data list
        animalCards = new ArrayList<>();

        // Set up RecyclerView
        setupRecyclerView();

        // Check authentication before loading cards
        if (!firebaseHelper.isUserAuthenticated()) {
            Log.w(TAG, "User not authenticated. Redirecting to login...");
            Toast.makeText(this, "Please login to view your collection", Toast.LENGTH_LONG).show();

            // Redirect to LoginActivity
            Intent loginIntent = new Intent(CollectionsActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish(); // Close this activity
            return;
        }

        // Load cards from Firebase
        loadCardsFromFirebase();
    }

    /**
     * Set up RecyclerView with adapter and layout manager
     */
    private void setupRecyclerView() {
        // Create adapter
        adapter = new AnimalCardAdapter(this, animalCards);

        // Set layout manager (vertical list)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set adapter
        recyclerView.setAdapter(adapter);

        // Optional: Set click listener for cards
        adapter.setOnCardClickListener(card -> {
            // Open detail activity
            openAnimalDetailActivity(card);
            Log.d(TAG, "Card clicked: " + card.getAnimalName());
        });

        Log.d(TAG, "RecyclerView set up successfully");
    }

    /**
     * Load user's animal cards from Firebase Firestore
     */
    private void loadCardsFromFirebase() {
        Log.d(TAG, "Loading user's cards from Firebase...");

        // Show loading indicator
        showLoading();

        // Fetch user-specific cards from Firestore
        firebaseHelper.fetchUserAnimalCards(new FirebaseHelper.FetchCallback() {
            @Override
            public void onSuccess(List<AnimalCard> cards) {
                Log.d(TAG, "Successfully loaded " + cards.size() + " cards");

                // Hide loading
                hideLoading();

                // Update data
                animalCards.clear();
                animalCards.addAll(cards);

                // Update adapter
                adapter.updateData(animalCards);

                // Show empty state if no cards
                if (cards.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                }

                Toast.makeText(CollectionsActivity.this,
                        "Loaded " + cards.size() + " cards",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to load cards: " + error);

                // Hide loading
                hideLoading();

                // Handle authentication error
                if ("USER_NOT_AUTHENTICATED".equals(error)) {
                    Toast.makeText(CollectionsActivity.this,
                            "Please login to view your collection",
                            Toast.LENGTH_LONG).show();

                    // Redirect to login
                    Intent loginIntent = new Intent(CollectionsActivity.this, LoginActivity.class);
                    startActivity(loginIntent);
                    finish();
                } else {
                    // Show error message
                    Toast.makeText(CollectionsActivity.this,
                            "Failed to load cards: " + error,
                            Toast.LENGTH_LONG).show();

                    // Show empty state
                    showEmptyState();
                }
            }
        });
    }

    /**
     * Show loading indicator
     */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    /**
     * Hide loading indicator
     */
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Show empty state when no cards exist
     */
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    /**
     * Hide empty state
     */
    private void hideEmptyState() {
        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Refresh cards (useful for pull-to-refresh feature)
     */
    public void refreshCards() {
        loadCardsFromFirebase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Optionally reload cards when returning to this activity
        // loadCardsFromFirebase();
    }

    /**
     * Open the detail screen for the selected animal card
     */
    private void openAnimalDetailActivity(AnimalCard card) {
        Intent intent = new Intent(this, AnimalDetailActivity.class);
        intent.putExtra("animal_id", card.getCardId()); // or use your correct field
        intent.putExtra("animal_name", card.getAnimalName());
        intent.putExtra("animal_image", card.getImageUrl()); // optional if you have one
        startActivity(intent);
    }

}