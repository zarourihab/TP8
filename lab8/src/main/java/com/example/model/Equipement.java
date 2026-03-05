package com.example.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "equipements")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Equipement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String nom;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    @Column(length = 500)
    private String description;

    @Column(name = "reference")
    private String reference;

    @ManyToMany(mappedBy = "equipements")
    private Set<Salle> salles = new HashSet<>();

    @Version
    private Long version;

    public Equipement() {}

    public Equipement(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    // Getters/Setters
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Set<Salle> getSalles() { return salles; }
    public Long getVersion() { return version; }
}