package com.petcare.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BreedTest {
    @Test
    void defaultConstructorAccessorsAndActiveDefault_PreserveRelationshipAndState() {
        Breed breed = new Breed(); Species species = new Species(); species.setId(1L);
        assertThat(breed.getActive()).isTrue();
        breed.setId(2L); breed.setSpecies(species); breed.setName("Poodle"); breed.setActive(false);
        assertThat(breed.getId()).isEqualTo(2L); assertThat(breed.getSpecies()).isSameAs(species);
        assertThat(breed.getName()).isEqualTo("Poodle"); assertThat(breed.getActive()).isFalse();
    }
}
