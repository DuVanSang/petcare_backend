package com.petcare.backend.service.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.model.Pet;
import com.petcare.backend.model.Post;
import com.petcare.backend.repository.PetTimelineEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetTimelineServiceImplTest {
    @Mock PetTimelineEventRepository events;
    @Test void createSocialPostEvent_NullAndMissingIds_DoNothing(){var service=new PetTimelineServiceImpl(events);service.createSocialPostEvent(null,new Post());Pet pet=new Pet();pet.setId(1L);service.createSocialPostEvent(pet,null);service.createSocialPostEvent(new Pet(),new Post());verify(events,never()).save(org.mockito.ArgumentMatchers.any());}
    @Test void createSocialPostEvent_ExistingAndNew_HandlesDuplicate(){var service=new PetTimelineServiceImpl(events);Pet pet=new Pet();pet.setId(1L);pet.setName("Milo");Post post=Post.builder().id(2L).build();when(events.existsByPetIdAndEventTypeAndReferenceId(1L,com.petcare.backend.model.PetTimelineEvent.EventType.social_post,2L)).thenReturn(true);service.createSocialPostEvent(pet,post);verify(events,never()).save(org.mockito.ArgumentMatchers.any());when(events.existsByPetIdAndEventTypeAndReferenceId(1L,com.petcare.backend.model.PetTimelineEvent.EventType.social_post,2L)).thenReturn(false);service.createSocialPostEvent(pet,post);verify(events).save(org.mockito.ArgumentMatchers.argThat(e->e.getPet()==pet&&e.getReferenceId()==2L));}
}
