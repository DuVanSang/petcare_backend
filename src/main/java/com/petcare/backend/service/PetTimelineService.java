package com.petcare.backend.service;

import com.petcare.backend.model.Pet;
import com.petcare.backend.model.Post;

public interface PetTimelineService {
    void createSocialPostEvent(Pet pet, Post post);
}
