package tn.limtic.limtic_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * §3.7 CDC — Modèle enrichi avec champs de classement réels :
 *  - facteurImpact  : Impact Factor (JCR/Clarivate)
 *  - scimagoQuartile: Q1 / Q2 / Q3 / Q4 (Scimago SJR)
 *  - snip           : Source Normalized Impact per Paper
 *  - classementCORE : A* / A / B / C  (CORE Ranking pour les conférences)
 */
@Data
@Entity
@Table(name = "publications")
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String type;   // "Journal" | "Conference" | "Book Chapter" | "Thesis"

    private int annee;
    private String journal;
    private String resume;
    private String lienUrl;
    private String doi;
    private String pdfUrl;
    private String motsCles;

    @Column(nullable = false)
    private String statut = "BROUILLON";  // BROUILLON | SOUMIS | PUBLIE

    // ── §3.7 Champs de classement ──────────────────────────────────────────
    /** Impact Factor JCR/Clarivate (ex: 4.58). Null si non renseigné. */
    private Double facteurImpact;

    /**
     * Quartile Scimago SJR : "Q1", "Q2", "Q3", "Q4".
     * Renseigné manuellement ou via import CSV.
     */
    private String scimagoQuartile;

    /**
     * Source Normalized Impact per Paper (SNIP).
     * Indicateur Scopus/Elsevier.
     */
    private Double snip;

    /**
     * Classement CORE pour les conférences : "A*", "A", "B", "C".
     * Voir http://portal.core.edu.au/conf-ranks/
     */
    private String classementCORE;

    /**
     * Source de référence du classement (ex: "Scimago 2024", "CORE 2023", "JCR Clarivate").
     * Permet de tracer l'origine du score renseigné.
     */
    private String sourceClassement;

    // ── Relations ──────────────────────────────────────────────────────────
    @ManyToOne
    @JoinColumn(name = "axe_id")
    @JsonIgnoreProperties("publications")
    private AxeRecherche axe;

    @ManyToMany(mappedBy = "publications")
    @JsonIgnoreProperties("publications")
    private List<Chercheur> chercheurs;
}