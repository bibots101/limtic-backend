package tn.limtic.limtic_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "axes_recherche")
public class AxeRecherche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "text")
    private String description;

    // Responsable de l'axe (un chercheur)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_id")
    @JsonIgnoreProperties({"axes", "publications", "user"})
    private Chercheur responsable;

    // Membres de l'axe — côté "inverse" : le @JoinTable est dans Chercheur.java
    @ManyToMany(mappedBy = "axes", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"axes", "publications", "user"})
    private List<Chercheur> chercheurs = new ArrayList<>();
}