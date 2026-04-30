package tn.limtic.limtic_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.limtic.limtic_backend.model.AxeRecherche;
import tn.limtic.limtic_backend.model.Chercheur;
import tn.limtic.limtic_backend.repository.AxeRechercheRepository;
import tn.limtic.limtic_backend.repository.ChercheurRepository;
import tn.limtic.limtic_backend.repository.PublicationRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/axes")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class AxeRechercheController {

    private final AxeRechercheRepository axeRepo;
    private final ChercheurRepository chercheurRepo;
    private final PublicationRepository publicationRepo;

    public AxeRechercheController(AxeRechercheRepository axeRepo,
                                   ChercheurRepository chercheurRepo,
                                   PublicationRepository publicationRepo) {
        this.axeRepo = axeRepo;
        this.chercheurRepo = chercheurRepo;
        this.publicationRepo = publicationRepo;
    }

    // ── GET tous les axes (public) ──────────────────────────
    @GetMapping
    public List<AxeRecherche> getAll() {
        return axeRepo.findAll();
    }

    // ── GET un axe par id (public) ──────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return axeRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── GET publications d'un axe (public) ─────────────────
    @GetMapping("/{id}/publications")
    public ResponseEntity<?> getPublications(@PathVariable Long id) {
        if (!axeRepo.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(publicationRepo.findByAxeId(id));
    }

    // ── POST créer un axe (admin) ───────────────────────────
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        AxeRecherche axe = new AxeRecherche();
        axe.setNom((String) body.get("nom"));
        axe.setDescription((String) body.get("description"));
        if (body.get("responsableId") != null) {
            Long rid = Long.valueOf(body.get("responsableId").toString());
            chercheurRepo.findById(rid).ifPresent(axe::setResponsable);
        }
        return ResponseEntity.ok(axeRepo.save(axe));
    }

    // ── PUT modifier un axe (admin) ─────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body) {
        Optional<AxeRecherche> opt = axeRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        AxeRecherche axe = opt.get();
        if (body.get("nom") != null)         axe.setNom((String) body.get("nom"));
        if (body.get("description") != null) axe.setDescription((String) body.get("description"));
        if (body.containsKey("responsableId")) {
            Object rid = body.get("responsableId");
            if (rid == null) {
                axe.setResponsable(null);
            } else {
                chercheurRepo.findById(Long.valueOf(rid.toString())).ifPresent(axe::setResponsable);
            }
        }
        return ResponseEntity.ok(axeRepo.save(axe));
    }

    // ── DELETE supprimer un axe (admin) ────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!axeRepo.existsById(id)) return ResponseEntity.notFound().build();
        axeRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Axe supprimé"));
    }

    // ── PUT remplacer tous les membres d'un axe (admin) ────
    @PutMapping("/{id}/chercheurs")
    public ResponseEntity<?> updateChercheurs(@PathVariable Long id,
                                               @RequestBody List<Long> chercheurIds) {
        Optional<AxeRecherche> opt = axeRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        // On modifie la relation du côté Chercheur (qui possède le @JoinTable)
        // D'abord on retire cet axe de tous les chercheurs
        List<Chercheur> tous = chercheurRepo.findAll();
        for (Chercheur c : tous) {
            if (c.getAxes() != null) {
                c.getAxes().removeIf(a -> a.getId().equals(id));
                chercheurRepo.save(c);
            }
        }
        // Ensuite on ajoute l'axe aux chercheurs sélectionnés
        AxeRecherche axe = opt.get();
        List<Chercheur> nouveaux = chercheurRepo.findAllById(chercheurIds);
        for (Chercheur c : nouveaux) {
            if (c.getAxes() == null) c.setAxes(new java.util.ArrayList<>());
            if (!c.getAxes().contains(axe)) {
                c.getAxes().add(axe);
                chercheurRepo.save(c);
            }
        }
        return ResponseEntity.ok(axeRepo.findById(id).get());
    }

    // ── POST ajouter un chercheur à un axe (admin) ──────────
    @PostMapping("/{axeId}/chercheurs/{chercheurId}")
    public ResponseEntity<?> addChercheur(@PathVariable Long axeId,
                                           @PathVariable Long chercheurId) {
        Optional<Chercheur> optC = chercheurRepo.findById(chercheurId);
        Optional<AxeRecherche> optA = axeRepo.findById(axeId);
        if (optC.isEmpty() || optA.isEmpty()) return ResponseEntity.notFound().build();
        Chercheur c = optC.get();
        AxeRecherche axe = optA.get();
        if (c.getAxes() == null) c.setAxes(new java.util.ArrayList<>());
        if (!c.getAxes().contains(axe)) {
            c.getAxes().add(axe);
            chercheurRepo.save(c);
        }
        return ResponseEntity.ok(Map.of("message", "Chercheur associé à l'axe"));
    }

    // ── DELETE retirer un chercheur d'un axe (admin) ────────
    @DeleteMapping("/{axeId}/chercheurs/{chercheurId}")
    public ResponseEntity<?> removeChercheur(@PathVariable Long axeId,
                                              @PathVariable Long chercheurId) {
        Optional<Chercheur> optC = chercheurRepo.findById(chercheurId);
        if (optC.isEmpty()) return ResponseEntity.notFound().build();
        Chercheur c = optC.get();
        if (c.getAxes() != null) {
            c.getAxes().removeIf(a -> a.getId().equals(axeId));
            chercheurRepo.save(c);
        }
        return ResponseEntity.ok(Map.of("message", "Chercheur retiré de l'axe"));
    }
}