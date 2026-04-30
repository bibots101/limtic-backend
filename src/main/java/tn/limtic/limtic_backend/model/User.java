package tn.limtic.limtic_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String nom;
    private String prenom;
    private String avatar;

    @Column(name = "cree_le")
    private LocalDateTime creeLe = LocalDateTime.now();

    public enum Role {
        VISITEUR, CHERCHEUR, ADMIN, SUPER_ADMIN
    }
}