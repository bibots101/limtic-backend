package tn.limtic.limtic_backend.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import tn.limtic.limtic_backend.model.AxeRecherche;
import tn.limtic.limtic_backend.model.Chercheur;
import tn.limtic.limtic_backend.repository.AxeRechercheRepository;
import tn.limtic.limtic_backend.repository.ChercheurRepository;
import tn.limtic.limtic_backend.repository.PublicationRepository;
import tn.limtic.limtic_backend.service.AuditService;

@RestController
@RequestMapping("/api/axes")
public class AxeRechercheController {

    private final AxeRechercheRepository axeRepo;
    private final ChercheurRepository chercheurRepo;
    private final PublicationRepository publicationRepo;
    private final AuditService auditService;

    public AxeRechercheController(AxeRechercheRepository axeRepo,
                                   ChercheurRepository chercheurRepo,
                                   PublicationRepository publicationRepo,
                                   AuditService auditService) {
        this.axeRepo = axeRepo;
        this.chercheurRepo = chercheurRepo;
        this.publicationRepo = publicationRepo;
        this.auditService = auditService;
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
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        AxeRecherche axe = new AxeRecherche();
        axe.setNom((String) body.get("nom"));
        axe.setDescription((String) body.get("description"));
        if (body.get("responsableId") != null) {
            Long rid = Long.valueOf(body.get("responsableId").toString());
            chercheurRepo.findById(rid).ifPresent(axe::setResponsable);
        }
        AxeRecherche saved = axeRepo.save(axe);
        auditService.log(request, "CREATE", "Axe", saved.getId(),
            "Axe de recherche créé : " + saved.getNom(), true);
        return ResponseEntity.ok(saved);
    }

    // ── PUT modifier un axe (admin) ─────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
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
        AxeRecherche saved = axeRepo.save(axe);
        auditService.log(request, "UPDATE", "Axe", id,
            "Axe de recherche modifié : " + saved.getNom(), true);
        return ResponseEntity.ok(saved);
    }

    // ── DELETE supprimer un axe (admin) ────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!axeRepo.existsById(id)) return ResponseEntity.notFound().build();
        String nom = axeRepo.findById(id).map(AxeRecherche::getNom).orElse("id=" + id);
        axeRepo.deleteById(id);
        auditService.log(request, "DELETE", "Axe", id,
            "Axe de recherche supprimé : " + nom, true);
        return ResponseEntity.ok(Map.of("message", "Axe supprimé"));
    }

    // ── PUT remplacer tous les membres d'un axe (admin) ────
    @PutMapping("/{id}/chercheurs")
    public ResponseEntity<?> updateChercheurs(@PathVariable Long id,
                                               @RequestBody List<Long> chercheurIds,
                                               HttpServletRequest request) {
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
        auditService.log(request, "UPDATE", "Axe", id,
            "Membres de l'axe \"" + axe.getNom() + "\" mis à jour (" + chercheurIds.size() + " membres)", true);
        return ResponseEntity.ok(axeRepo.findById(id).get());
    }

    // ── POST ajouter un chercheur à un axe (admin) ──────────
    @PostMapping("/{axeId}/chercheurs/{chercheurId}")
    public ResponseEntity<?> addChercheur(@PathVariable Long axeId,
                                           @PathVariable Long chercheurId,
                                           HttpServletRequest request) {
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
        auditService.log(request, "UPDATE", "Axe", axeId,
            "Chercheur #" + chercheurId + " ajouté à l'axe \"" + axe.getNom() + "\"", true);
        return ResponseEntity.ok(Map.of("message", "Chercheur associé à l'axe"));
    }

    // ── DELETE retirer un chercheur d'un axe (admin) ────────
    @DeleteMapping("/{axeId}/chercheurs/{chercheurId}")
    public ResponseEntity<?> removeChercheur(@PathVariable Long axeId,
                                              @PathVariable Long chercheurId,
                                              HttpServletRequest request) {
        Optional<Chercheur> optC = chercheurRepo.findById(chercheurId);
        if (optC.isEmpty()) return ResponseEntity.notFound().build();
        Chercheur c = optC.get();
        String axeNom = axeRepo.findById(axeId).map(AxeRecherche::getNom).orElse("id=" + axeId);
        if (c.getAxes() != null) {
            c.getAxes().removeIf(a -> a.getId().equals(axeId));
            chercheurRepo.save(c);
        }
        auditService.log(request, "UPDATE", "Axe", axeId,
            "Chercheur #" + chercheurId + " retiré de l'axe \"" + axeNom + "\"", true);
        return ResponseEntity.ok(Map.of("message", "Chercheur retiré de l'axe"));
    }
}