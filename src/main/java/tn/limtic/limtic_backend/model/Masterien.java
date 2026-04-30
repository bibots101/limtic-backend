package tn.limtic.limtic_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "masteriens")
public class Masterien {

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

    @Column(name = "sujet_memoire")
    private String sujetMemoire;

    @ManyToOne
    @JoinColumn(name = "encadrant_id")
    private Chercheur encadrant;

    @Column(name = "promotion")
    private String promotion;

    @Column(name = "statut")
    private String statut = "EN_COURS";

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getSujetMemoire() { return sujetMemoire; }
    public void setSujetMemoire(String sujetMemoire) { this.sujetMemoire = sujetMemoire; }
    public Chercheur getEncadrant() { return encadrant; }
    public void setEncadrant(Chercheur encadrant) { this.encadrant = encadrant; }
    public String getPromotion() { return promotion; }
    public void setPromotion(String promotion) { this.promotion = promotion; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}