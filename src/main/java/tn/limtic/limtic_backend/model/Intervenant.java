package tn.limtic.limtic_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Intervenant invité à un événement.
 */
@Data
@Entity
@Table(name = "intervenants")
public class Intervenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String prenom;
    private String affiliation;     // Institution / entreprise
    private String titrePresentation;
    private String email;
    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id", nullable = false)
    @JsonIgnoreProperties({"intervenants", "photos"})
    private Evenement evenement;
}
