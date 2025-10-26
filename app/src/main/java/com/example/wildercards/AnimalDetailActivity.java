package com.example.wildercards;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AnimalDetailActivity extends BaseActivity {

    private static final String TAG = "AnimalDetailActivity";

    // UI Components
    private TextView tvTitle;
    private TextView tvCoinsEarned;
    private ImageView ivCoinIcon;
    private ImageButton btnDelete;
    private ImageView ivGeneratedCard;
    private ImageView ivWikipediaImage;
    private TextView tvCollectionDate;
    private TextView tvScientificName;
    private TextView tvDescription;
    private TextView tvHabitat;
    private TextView tvConservationStatus;
    private CardView cardWikipediaImage;
    private NestedScrollView scrollView;

    // Data
    private AnimalCard animalCard;
    private String cardId;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_detail);

        Log.d(TAG, "AnimalDetailActivity created");

        // Initialize Firebase
        firebaseHelper = new FirebaseHelper();

        // Initialize views
        initViews();

        // Get data from intent
        loadDataFromIntent();

        // Display data
        displayAnimalData();

        // Setup delete button
        setupDeleteButton();

        // Start entrance animations
        startEntranceAnimations();
    }

    /**
     * Initialize all views
     */
    private void initViews() {
        tvTitle = findViewById(R.id.tvAnimalTitle);
        tvCoinsEarned = findViewById(R.id.tvCoinsEarned);
        ivCoinIcon = findViewById(R.id.ivCoinIconDetail);
        btnDelete = findViewById(R.id.btnDelete);
        ivGeneratedCard = findViewById(R.id.ivGeneratedCard);
        ivWikipediaImage = findViewById(R.id.ivWikipediaImage);
        tvCollectionDate = findViewById(R.id.tvCollectionDate);
        tvScientificName = findViewById(R.id.tvScientificName);
        tvDescription = findViewById(R.id.tvDescription);
        tvHabitat = findViewById(R.id.tvHabitat);
        tvConservationStatus = findViewById(R.id.tvConservationStatus);
        cardWikipediaImage = findViewById(R.id.cardWikipediaImage);
        scrollView = findViewById(R.id.scrollView);
    }

    /**
     * Load AnimalCard data from intent
     */
    private void loadDataFromIntent() {
        // Get card ID
        cardId = getIntent().getStringExtra("CARD_ID");

        // Get card data (you'll need to pass this via Intent)
        String animalName = getIntent().getStringExtra("ANIMAL_NAME");
        String description = getIntent().getStringExtra("DESCRIPTION");
        String imageUrl = getIntent().getStringExtra("IMAGE_URL");
        String sciName = getIntent().getStringExtra("SCIENTIFIC_NAME");
        String habitat = getIntent().getStringExtra("HABITAT");
        String conservation = getIntent().getStringExtra("CONSERVATION");
        int coinsEarned = getIntent().getIntExtra("COINS_EARNED", 5);
        long timestamp = getIntent().getLongExtra("TIMESTAMP", System.currentTimeMillis());

        // Create AnimalCard object
        animalCard = new AnimalCard();
        animalCard.setCardId(cardId);
        animalCard.setAnimalName(animalName);
        animalCard.setDescription(description);
        animalCard.setImageUrl(imageUrl);
        animalCard.setSciName(sciName);
        animalCard.setHabitat(habitat);
        animalCard.setConservation(conservation);
        // animalCard.setCoinsEarned(coinsEarned);
        animalCard.setTimestamp(timestamp);

        Log.d(TAG, "Loaded animal: " + animalName + " (ID: " + cardId + ")");
    }

    /**
     * Display all animal data
     */
    private void displayAnimalData() {
        if (animalCard == null) {
            Log.e(TAG, "AnimalCard is null!");
            Toast.makeText(this, "Error loading card data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set title
        tvTitle.setText(animalCard.getAnimalName());

        // Set coins earned
//        int coins = animalCard.getCoinsEarned();
//        if (coins <= 0) {
//            // Calculate from conservation status if not stored
//            coins = ConservationStatusMapper.getCoinsForFullText(animalCard.getConservation());
//        }
//        tvCoinsEarned.setText("+" + coins);

        // Set collection date
        // tvCollectionDate.setText(animalCard.getFormattedCollectionDate());

        // Load generated card image
        if (animalCard.getImageUrl() != null && !animalCard.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(animalCard.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(ivGeneratedCard);

            // Add click listener for zoom
            ivGeneratedCard.setOnClickListener(v -> zoomImage(ivGeneratedCard));
        }

        // Load Wikipedia image in background
        loadWikipediaImage();

        // Set scientific name
        String sciName = animalCard.getSciName();
        if (sciName != null && !sciName.isEmpty() && !sciName.equals("Unknown")) {
            tvScientificName.setText(sciName);
        } else {
            tvScientificName.setText("Scientific name unknown");
        }

        // Set description
        String description = animalCard.getDescription();
        if (description != null && !description.isEmpty()) {
            tvDescription.setText(description);
        } else {
            tvDescription.setText("No description available.");
        }

        // Set habitat
        String habitat = animalCard.getHabitat();
        if (habitat != null && !habitat.isEmpty()) {
            tvHabitat.setText(habitat);
        } else {
            tvHabitat.setText("Habitat information not available.");
        }

        // Set conservation status with badge
        String conservation = animalCard.getConservation();
        if (conservation != null && !conservation.isEmpty()) {
            String abbrev = ConservationStatusMapper.mapFullTextToAbbreviation(conservation);
            String emoji = ConservationStatusMapper.getRarityEmoji(abbrev);
            String rarity = ConservationStatusMapper.getRarityName(abbrev);

            tvConservationStatus.setText(emoji + " " + conservation + " (" + abbrev + ") - " + rarity);

            // Apply rarity badge styling
            RarityBadgeHelper.applyRarityBadge(tvConservationStatus, conservation);
        } else {
            tvConservationStatus.setText("🟢 Status Unknown");
        }
    }

    /**
     * Load Wikipedia image for the animal
     */
    private void loadWikipediaImage() {
        new Thread(() -> {
            AnimalInfo info = WikipediaFetcher.fetchAnimalInfo(animalCard.getAnimalName());

            runOnUiThread(() -> {
                if (info != null && info.getImageUrl() != null && !info.getImageUrl().isEmpty()) {
                    // Show Wikipedia image
                    cardWikipediaImage.setVisibility(View.VISIBLE);

                    Glide.with(this)
                            .load(info.getImageUrl())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(ivWikipediaImage);

                    // Add click listener for zoom
                    ivWikipediaImage.setOnClickListener(v -> zoomImage(ivWikipediaImage));

                    Log.d(TAG, "Wikipedia image loaded");
                } else {
                    // Hide Wikipedia image section if not available
                    cardWikipediaImage.setVisibility(View.GONE);
                    Log.d(TAG, "No Wikipedia image available");
                }
            });
        }).start();
    }

    /**
     * Setup delete button
     */
    private void setupDeleteButton() {
        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });
    }

    /**
     * Show confirmation dialog before deleting
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Card?")
                .setMessage("Are you sure you want to delete \"" + animalCard.getAnimalName() + "\" from your collection?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteCard();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete card from Firebase
     */
    private void deleteCard() {
        if (cardId == null || cardId.isEmpty()) {
            Toast.makeText(this, "Error: Card ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        btnDelete.setEnabled(false);
        Toast.makeText(this, "Deleting...", Toast.LENGTH_SHORT).show();

        firebaseHelper.deleteAnimalCard(cardId, new FirebaseHelper.DeleteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Card deleted successfully");
                Toast.makeText(AnimalDetailActivity.this, "Card deleted successfully", Toast.LENGTH_SHORT).show();

                // Animate out and finish
                animateDeleteAndFinish();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to delete card: " + error);
                Toast.makeText(AnimalDetailActivity.this, "Delete failed: " + error, Toast.LENGTH_SHORT).show();
                btnDelete.setEnabled(true);
            }
        });
    }

    /**
     * Animate card deletion and finish activity
     */
    private void animateDeleteAndFinish() {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(scrollView, "alpha", 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(scrollView, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(scrollView, "scaleY", 1f, 0.8f);

        fadeOut.setDuration(300);
        scaleX.setDuration(300);
        scaleY.setDuration(300);

        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finish();
            }
        });

        fadeOut.start();
        scaleX.start();
        scaleY.start();
    }

    /**
     * Start entrance animations
     */
    private void startEntranceAnimations() {
        // Fade in scroll view
        scrollView.setAlpha(0f);
        scrollView.animate()
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Animate generated card image
        ivGeneratedCard.setScaleX(0.8f);
        ivGeneratedCard.setScaleY(0.8f);
        ivGeneratedCard.setAlpha(0f);

        ivGeneratedCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Zoom animation for images
     */
    private void zoomImage(ImageView imageView) {
        if (imageView.getScaleX() == 1f) {
            // Zoom in
            imageView.animate()
                    .scaleX(2f)
                    .scaleY(2f)
                    .setDuration(300)
                    .start();
        } else {
            // Zoom out
            imageView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start();
        }
    }
    //onbackpressed
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        finish();
//        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
//        //finish();
//
//
//    }

//    @Override
//    public void onBackPressed() {
//        // Smooth exit animation
//        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(scrollView, "alpha", 1f, 0f);
//        fadeOut.setDuration(200);
//        fadeOut.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                AnimalDetailActivity.super.onBackPressed();
//            }
//        });
//        fadeOut.start();
//    }

    public String getFormattedCollectionDate() {
//        Date date = new Date(timestamp);
//        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
          return null;
        // }//
    }

}