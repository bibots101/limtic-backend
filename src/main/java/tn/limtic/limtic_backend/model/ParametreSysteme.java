package tn.limtic.limtic_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * §4.3.6 CDC — Paramètres système configurables via l'interface d'administration.
 *
 * Au lieu de tout mettre dans application.properties (non modifiable à chaud),
 * on stocke les paramètres sensibles/configurables en base de données.
 *
 * Exemples de clés :
 *   labo.nom              → "LIMTIC"
 *   labo.acronyme         → "LIMTIC"
 *   labo.description      → "Laboratoire d'Informatique..."
 *   labo.email            → "contact@limtic.tn"
 *   labo.telephone        → "+216 71 000 000"
 *   labo.adresse          → "ISI, Université de Tunis El Manar"
 *   labo.logoUrl          → "/uploads/logo.png"
 *   seo.titre             → "LIMTIC - Laboratoire de Recherche"
 *   seo.description       → "Meta description pour Google"
 *   seo.mots_cles         → "intelligence artificielle, IoT, machine learning"
 *   smtp.host             → "smtp.gmail.com"
 *   smtp.port             → "587"
 *   smtp.username         → "xxx@gmail.com"
 *   smtp.password         → "xxxx xxxx xxxx xxxx"  (stocké chiffré en production)
 *   smtp.destinataire     → "admin@limtic.tn"
 *   contact.google_maps   → "https://maps.google.com/?q=..."
 *   contact.latitude      → "36.8447"
 *   contact.longitude     → "10.1942"
 *   reseaux.linkedin      → "https://linkedin.com/company/limtic"
 *   reseaux.researchgate  → "..."
 */
@Data
@Entity
@Table(name = "parametres_systeme",
       uniqueConstraints = @UniqueConstraint(columnNames = "cle"))
public class ParametreSysteme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Clé du paramètre (ex: "labo.nom") */
    @Column(nullable = false, unique = true, length = 100)
    private String cle;

    /** Valeur du paramètre */
    @Column(columnDefinition = "TEXT")
    private String valeur;

    /** Description affichée dans l'UI admin */
    private String description;

    /** Groupe d'affichage dans l'UI (labo, seo, smtp, contact, reseaux) */
    private String groupe;

    /** Si true, la valeur est masquée dans l'UI (pour les mots de passe) */
    private boolean sensible = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creeLe = LocalDateTime.now();

    private LocalDateTime modifieLe;

    @PreUpdate
    public void preUpdate() {
        this.modifieLe = LocalDateTime.now();
    }
}
