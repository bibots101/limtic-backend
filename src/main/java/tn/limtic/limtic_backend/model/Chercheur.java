package tn.limtic.limtic_backend.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "chercheurs")
public class Chercheur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String grade;
    private String institution;
    private String specialite;
    private String photoUrl;
    private String cvUrl;

    private String bureau;
    private String telephone;
    private String biographie;
    private String googleScholar;
    private String researchGate;
    private String orcid;
    private String linkedin;

    @ManyToMany
    @JoinTable(
        name = "chercheur_publication",
        joinColumns = @JoinColumn(name = "chercheur_id"),
        inverseJoinColumns = @JoinColumn(name = "publication_id")
    )
    @JsonIgnoreProperties({"chercheurs", "axe"})
    private List<Publication> publications;

    // C'est CE côté qui possède le @JoinTable pour chercheur_axe
    @ManyToMany
    @JoinTable(
        name = "chercheur_axe",
        joinColumns = @JoinColumn(name = "chercheur_id"),
        inverseJoinColumns = @JoinColumn(name = "axe_id")
    )
    @JsonIgnoreProperties({"chercheurs", "responsable"})
    private List<AxeRecherche> axes;
}