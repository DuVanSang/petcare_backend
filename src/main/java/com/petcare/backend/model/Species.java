package com.petcare.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories_species")
@Getter
@Setter
@NoArgsConstructor
public class Species {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active = true;

    @OneToMany(mappedBy = "species", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Breed> breeds = new ArrayList<>();
}
