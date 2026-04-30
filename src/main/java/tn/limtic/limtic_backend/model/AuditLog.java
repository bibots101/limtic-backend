package tn.limtic.limtic_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * §4.1 CDC — Journal d'audit des actions critiques.
 * Enregistre : qui, quoi, sur quelle entité, quand, depuis quelle IP.
 */
@Data
@Entity
@Table(name = "audit_logs",
       indexes = {
           @Index(name = "idx_audit_date",   columnList = "dateAction"),
           @Index(name = "idx_audit_action", columnList = "action"),
           @Index(name = "idx_audit_user",   columnList = "userEmail")
       })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email de l'utilisateur qui a effectué l'action (null si anonyme) */
    private String userEmail;

    /**
     * Type d'action : LOGIN, LOGOUT, CREATE, UPDATE, DELETE,
     * EXPORT, IMPORT, PUBLISH, RESET_PASSWORD, etc.
     */
    @Column(nullable = false)
    private String action;

    /** Entité concernée : "Publication", "Chercheur", "Evenement", ... */
    private String entite;

    /** ID de l'objet concerné (ex : id de la publication supprimée) */
    private Long entiteId;

    /** Description lisible de l'action */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** Adresse IP du client */
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    /** true = succès, false = échec (ex : tentative de login ratée) */
    private boolean succes = true;
}
