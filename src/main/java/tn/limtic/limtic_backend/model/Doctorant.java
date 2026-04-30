package tn.limtic.limtic_backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "doctorants")
public class Doctorant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "nom")
    private String nom;

    @Column(name = "prenom")
    private String prenom;

    @Column(name = "sujet_these")
    private String sujetThese;

    @ManyToOne
    @JoinColumn(name = "directeur_id")
    private Chercheur directeur;

    @Column(name = "date_inscription")
    private LocalDate dateInscription;

    @Column(name = "date_soutenance")
    private LocalDate dateSoutenance;

    @Column(name = "statut")
    private String statut = "EN_COURS";

    @Column(name = "mention")
    private String mention;

    @Column(name = "photo_url")
    private String photoUrl;

    // Getters & Setters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getSujetThese() { return sujetThese; }
    public void setSujetThese(String sujetThese) { this.sujetThese = sujetThese; }
    public Chercheur getDirecteur() { return directeur; }
    public void setDirecteur(Chercheur directeur) { this.directeur = directeur; }
    public LocalDate getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }
    public LocalDate getDateSoutenance() { return dateSoutenance; }
    public void setDateSoutenance(LocalDate dateSoutenance) { this.dateSoutenance = dateSoutenance; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getMention() { return mention; }
    public void setMention(String mention) { this.mention = mention; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}