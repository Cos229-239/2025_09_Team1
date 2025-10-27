package com.example.wildercards;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

/**
 * Adapter for displaying animal cards in RecyclerView
 */
public class AnimalCardAdapter extends RecyclerView.Adapter<AnimalCardAdapter.CardViewHolder> {

    private static final String TAG = "AnimalCardAdapter";
    private Context context;
    private List<AnimalCard> animalCards;
    private OnCardClickListener clickListener;

    // Constructor
    public AnimalCardAdapter(Context context, List<AnimalCard> animalCards) {
        this.context = context;
        this.animalCards = animalCards;
    }

    // Optional: Click listener interface for future features
    public interface OnCardClickListener {
        void onCardClick(AnimalCard card);
    }

    public void setOnCardClickListener(OnCardClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_animal_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        AnimalCard card = animalCards.get(position);

        Log.d(TAG, "Binding card at position " + position + ": " + card.getAnimalName());

        // Set animal name
        holder.animalName.setText(card.getAnimalName());

        // Set scientific name (or "Unknown" if empty)
        String sciName = card.getSciName();
        if (sciName != null && !sciName.isEmpty() && !sciName.equals("Unknown")) {
            holder.scientificName.setText(sciName);
            holder.scientificName.setVisibility(View.VISIBLE);
        } else {
            holder.scientificName.setVisibility(View.GONE);
        }

        // Set first sentence of description
        String description = getFirstSentence(card.getDescription());
        holder.description.setText(description);

        // Load image using Glide
        if (card.getImageUrl() != null && !card.getImageUrl().isEmpty()) {
            Log.d(TAG, "Loading image: " + card.getImageUrl());

            Glide.with(context)
                    .load(card.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background) // FIX: Replace with your placeholder
                    .error(R.drawable.ic_launcher_foreground) // FIX: Replace with your error image
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(holder.animalImage);
        } else {
            Log.w(TAG, "No image URL for card: " + card.getAnimalName());
            holder.animalImage.setImageResource(R.drawable.ic_launcher_background); // FIX: Placeholder
        }

        // Handle card clicks (optional - for future detail view)
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCardClick(card);
            }
        });
    }

    @Override
    public int getItemCount() {
        return animalCards != null ? animalCards.size() : 0;
    }

    /**
     * Extract the first sentence from a description
     * Looks for sentence endings: . ? !
     */
    private String getFirstSentence(String description) {
        if (description == null || description.isEmpty()) {
            return "No description available.";
        }

        // Find the first sentence ending
        int endIndex = -1;

        // Look for ". " (period followed by space)
        int periodIndex = description.indexOf(". ");
        if (periodIndex != -1) endIndex = periodIndex + 1;

        // Look for "? " or "! " if no period found
        if (endIndex == -1) {
            int questionIndex = description.indexOf("? ");
            if (questionIndex != -1) endIndex = questionIndex + 1;
        }

        if (endIndex == -1) {
            int exclamationIndex = description.indexOf("! ");
            if (exclamationIndex != -1) endIndex = exclamationIndex + 1;
        }

        // If no sentence ending found, return first 150 characters
        if (endIndex == -1) {
            if (description.length() > 150) {
                return description.substring(0, 150) + "...";
            }
            return description;
        }

        return description.substring(0, endIndex).trim();
    }

    /**
     * Update the adapter with new data
     */
    public void updateData(List<AnimalCard> newCards) {
        this.animalCards = newCards;
        notifyDataSetChanged();
        Log.d(TAG, "Adapter updated with " + newCards.size() + " cards");
    }

    /**
     * ViewHolder class - holds references to views
     */
    static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView animalImage;
        TextView animalName;
        TextView scientificName;
        TextView description;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            animalImage = itemView.findViewById(R.id.animalImage);
            animalName = itemView.findViewById(R.id.animalName);
            scientificName = itemView.findViewById(R.id.scientificName);
            description = itemView.findViewById(R.id.description);
        }
    }
}