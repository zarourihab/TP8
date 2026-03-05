package com.example.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;

@Entity
@Table(name = "salles")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String nom;

    @NotNull(message = "La capacité est obligatoire")
    @Min(value = 1, message = "La capacité minimum est de 1 personne")
    @Column(nullable = false)
    private Integer capacite;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    @Column(length = 500)
    private String description;

    @Column(name = "batiment")
    private String batiment;

    @Column(name = "etage")
    private Integer etage;

    @Column(name = "numero")
    private String numero;

    @OneToMany(mappedBy = "salle", cascade = CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Reservation> reservations = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "salle_equipement",
            joinColumns = @JoinColumn(name = "salle_id"),
            inverseJoinColumns = @JoinColumn(name = "equipement_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Equipement> equipements = new HashSet<>();

    @Version
    private Long version;

    public Salle() {}

    public Salle(String nom, Integer capacite) {
        this.nom = nom;
        this.capacite = capacite;
    }

    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
        reservation.setSalle(this);
    }

    public void addEquipement(Equipement equipement) {
        equipements.add(equipement);
        equipement.getSalles().add(this);
    }

    public void removeEquipement(Equipement equipement) {
        equipements.remove(equipement);
        equipement.getSalles().remove(this);
    }

    // Getters/Setters
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Integer getCapacite() { return capacite; }
    public void setCapacite(Integer capacite) { this.capacite = capacite; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBatiment() { return batiment; }
    public void setBatiment(String batiment) { this.batiment = batiment; }
    public Integer getEtage() { return etage; }
    public void setEtage(Integer etage) { this.etage = etage; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public List<Reservation> getReservations() { return reservations; }
    public Set<Equipement> getEquipements() { return equipements; }
    public Long getVersion() { return version; }
}