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

    // Firebase Helper
    private FirebaseHelper firebaseHelper;

    private RecyclerView recyclerView;
    private AnimalAdapter animalAdapter;
    private List<TopCards> animalList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.animalsRecyclerView);

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

        // Initialize Firebase
        firebaseHelper = new FirebaseHelper();

        // Load cards from Firebase
        loadHomeData();

        // animalsContainer = findViewById(R.id.animalsContainer);



    }

    /**
     * Load card data from Firebase for home screen
     */
    private void loadHomeData() {
        Log.d(TAG, "Loading home data from Firebase...");

        firebaseHelper.fetchAllAnimalCards(new FirebaseHelper.FetchCallback() {
            @Override
            public void onSuccess(List<AnimalCard> cards) {
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
                Log.e(TAG, "Failed to load home data: " + error);
                Toast.makeText(MainActivity.this,
                        "Failed to load cards: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Display the top/featured card (last saved card)
     */
    private void displayTopCard(AnimalCard card) {
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
        // Inflate the item layout
        View cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_home_collection_card, collectionsContainer, false);

        // Get views
        ImageView cardImage = cardView.findViewById(R.id.collectionCardImage);
        TextView cardBrand = cardView.findViewById(R.id.collectionCardBrand);
        TextView cardName = cardView.findViewById(R.id.collectionCardName);

        // Set data
        cardName.setText(card.getAnimalName());

        // Set brand/scientific name
        String sciName = card.getSciName();
        if (sciName != null && !sciName.isEmpty() && !sciName.equals("Unknown")) {
            cardBrand.setText(sciName);
        } else {
            cardBrand.setText("Wildlife");
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
        // FIX: Reload data when returning to home screen
        loadHomeData();
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