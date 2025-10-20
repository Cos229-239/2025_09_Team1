package com.example.wildercards;

import java.util.List;

public class AnimalResponse {
    private List<TopCards> animals;

    public List<TopCards> getAnimals() {
        return animals;
    }

    public void setAnimals(List<TopCards> animals) {
        this.animals = animals;
    }
}