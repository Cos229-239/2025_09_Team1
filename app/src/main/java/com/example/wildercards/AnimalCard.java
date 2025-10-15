package com.example.wildercards;

public class AnimalCard {
    private String animalName;
    private String description;
    private String imageUrl;
    private long timestamp;
    private String cardId;

    // Empty constructor needed for Firestore
    public AnimalCard() {
    }

    public AnimalCard(String animalName, String description, String imageUrl) {
        this.animalName = animalName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getAnimalName() {
        return animalName;
    }

    public void setAnimalName(String animalName) {
        this.animalName = animalName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }
}