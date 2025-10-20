package com.example.wildercards;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class AnimalAdapter extends RecyclerView.Adapter<AnimalAdapter.AnimalViewHolder> {

    private List<TopCards> animals;
    private Context context;

    public AnimalAdapter(Context context, List<TopCards> animals) {
        this.context = context;
        this.animals = animals;
    }

    @NonNull
    @Override
    public AnimalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.animal_item, parent, false);
        return new AnimalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimalViewHolder holder, int position) {
        TopCards animal = animals.get(position);
        holder.animalName.setText(animal.getName());

        // Get drawable resource ID from string name
        int resourceId = context.getResources().getIdentifier(
                animal.getImageResource(),
                "drawable",
                context.getPackageName()
        );

        // Load image with Glide
        if (resourceId != 0) {
            Glide.with(context)
                    .load(resourceId)
                    .into(holder.animalImage);
        } else {
            // Fallback if resource not found
            holder.animalImage.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    @Override
    public int getItemCount() {
        return animals != null ? animals.size() : 0;
    }

    public static class AnimalViewHolder extends RecyclerView.ViewHolder {
        ImageView animalImage;
        TextView animalName;

        public AnimalViewHolder(@NonNull View itemView) {
            super(itemView);
            animalImage = itemView.findViewById(R.id.animalImage);
            animalName = itemView.findViewById(R.id.animalName);
        }
    }
}
