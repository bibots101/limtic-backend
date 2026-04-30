package tn.limtic.limtic_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Photo liée à un événement.
 * L'upload de fichier est géré par EvenementController (multipart).
 * L'URL stockée est le chemin relatif servi par Spring (/uploads/evenements/xxx.jpg).
 */
@Data
@Entity
@Table(name = "photos_evenements")
public class PhotoEvenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Chemin relatif ou URL complète de la photo */
    @Column(nullable = false)
    private String url;

    /** Légende affichée sous la photo */
    private String legende;

    /** Ordre d'affichage dans la galerie (drag-drop) */
    private int ordre = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id", nullable = false)
    @JsonIgnoreProperties({"photos", "intervenants"})
    private Evenement evenement;
}
