package com.petcare.backend.util;

import com.petcare.backend.model.Breed;
import java.util.Set;

public final class BreedCategoryHelper {
    private static final Set<String> OTHER_BREED_NAMES = Set.of("Khác", "Hỗn hợp / Không rõ");

    private BreedCategoryHelper() {
    }

    public static boolean isOtherBreed(String breedName) {
        return breedName != null && OTHER_BREED_NAMES.contains(breedName);
    }

    public static boolean isOtherBreed(Breed breed) {
        return breed != null && isOtherBreed(breed.getName());
    }

    public static String displayBreedName(Breed breed, String customBreedName) {
        if (isOtherBreed(breed) && customBreedName != null && !customBreedName.isBlank()) {
            return customBreedName.trim();
        }
        return breed != null ? breed.getName() : null;
    }
}
