package tn.limtic.limtic_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle Evenement enrichi :
 *  - galerie de photos (collection d'URLs stockées en DB)
 *  - intervenants invités
 *  - programme PDF
 *  - date de fin
 *  - statut (PLANIFIE, EN_COURS, TERMINE, ANNULE)
 */
@Data
@Entity
@Table(name = "evenements")
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    private LocalDate dateEvenement;
    private LocalDate dateFin;          // ← nouveau : date de fin pour les événements multi-jours
    private String lieu;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String type;                // "Séminaire" | "Workshop" | "Soutenance" | "Conférence"

    private String statut = "PLANIFIE"; // PLANIFIE | EN_COURS | TERMINE | ANNULE

    /** URL du programme PDF (upload serveur ou lien externe) */
    private String programmeUrl;

    /** Programme scientifique saisi en texte riche HTML */
    @Column(columnDefinition = "TEXT")
    private String programmeTexte;

    // ── Galerie photos ─────────────────────────────────────────────────────
    /**
     * Photos de l'événement.
     * Chaque PhotoEvenement est une entité séparée avec url, légende, ordre.
     * Cascade ALL + orphanRemoval pour suppression automatique.
     */
    @OneToMany(mappedBy = "evenement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    private List<PhotoEvenement> photos = new ArrayList<>();

    // ── Intervenants ───────────────────────────────────────────────────────
    /**
     * Intervenants invités (nom, affiliation, titre de la présentation).
     */
    @OneToMany(mappedBy = "evenement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Intervenant> intervenants = new ArrayList<>();
}
