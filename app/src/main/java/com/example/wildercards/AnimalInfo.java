package com.example.wildercards;


public class AnimalInfo {
    private String name;
    private String scientificName;
    private String description;
    private String imageUrl;
    private String habitat;
    private String conservationStatus;

    public AnimalInfo() {} // Needed for Firebase

    public AnimalInfo(String name, String scientificName, String description,
                      String imageUrl, String habitat, String conservationStatus) {
        this.name = name;
        this.scientificName = scientificName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.habitat = habitat;
        this.conservationStatus = conservationStatus;
    }

    // Getters and Setters
    public String getName() { return name; }
    public String getScientificName() { return scientificName; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getHabitat() { return habitat; }
    public String getConservationStatus() { return conservationStatus; }

    public void setName(String name) { this.name = name; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setHabitat(String habitat) { this.habitat = habitat; }
    public void setConservationStatus(String conservationStatus) { this.conservationStatus = conservationStatus; }
}
