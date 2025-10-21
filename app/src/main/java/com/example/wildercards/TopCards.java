package com.example.wildercards;

//public class TopCards {
//    private String name;
//    private String imageUrl;
//
//    public String getName() { return name; }
//    public String getImageUrl() { return imageUrl; }
//}


public class TopCards {
    private String name;
    private String imageResource;

    public TopCards(String name, String imageResource) {
        this.name = name;
        this.imageResource = imageResource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageResource() {
        return imageResource;
    }

    public void setImageResource(String imageResource) {
        this.imageResource = imageResource;
    }
}