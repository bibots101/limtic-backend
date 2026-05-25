package tn.limtic.limtic_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.limtic.limtic_backend.model.Doctorant;
import tn.limtic.limtic_backend.repository.DoctorantRepository;
import tn.limtic.limtic_backend.repository.ChercheurRepository;
import tn.limtic.limtic_backend.service.AuditService;
import tn.limtic.limtic_backend.service.FileStorageService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctorants")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class DoctorantController {

    private final DoctorantRepository doctorantRepo;
    private final ChercheurRepository chercheurRepo;
    private final AuditService auditService;
    private final FileStorageService storageService;

    public DoctorantController(DoctorantRepository doctorantRepo,
                                ChercheurRepository chercheurRepo,
                                AuditService auditService,
                                FileStorageService storageService) {
        this.doctorantRepo = doctorantRepo;
        this.chercheurRepo = chercheurRepo;
        this.auditService = auditService;
        this.storageService = storageService;
    }

    @GetMapping
    public List<Doctorant> getAll() { return doctorantRepo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return doctorantRepo.findById(id).map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Doctorant d = new Doctorant();
        d.setNom((String) body.get("nom"));
        d.setPrenom((String) body.get("prenom"));
        d.setSujetThese((String) body.get("sujetThese"));
        d.setStatut((String) body.getOrDefault("statut", "EN_COURS"));
        d.setMention((String) body.get("mention"));
        d.setPhotoUrl((String) body.get("photoUrl"));
        if (body.get("dateInscription") != null) {
            String dateInscription = body.get("dateInscription").toString().trim();
            if (!dateInscription.isEmpty()) {
                d.setDateInscription(java.time.LocalDate.parse(dateInscription));
            }
        }
        if (body.get("dateSoutenance") != null) {
            String dateSoutenance = body.get("dateSoutenance").toString().trim();
            if (!dateSoutenance.isEmpty()) {
                d.setDateSoutenance(java.time.LocalDate.parse(dateSoutenance));
            }
        }
        Long directeurId = parseLong(body.get("directeurId"));
        if (directeurId != null)
            chercheurRepo.findById(directeurId).ifPresent(d::setDirecteur);
        Doctorant saved = doctorantRepo.save(d);
        auditService.log(request, "CREATE", "Doctorant", saved.getId(),
            "Doctorant créé : " + saved.getPrenom() + " " + saved.getNom(), true);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        Optional<Doctorant> opt = doctorantRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Doctorant d = opt.get();
        if (body.get("nom") != null)        d.setNom((String) body.get("nom"));
        if (body.get("prenom") != null)     d.setPrenom((String) body.get("prenom"));
        if (body.get("sujetThese") != null) d.setSujetThese((String) body.get("sujetThese"));
        if (body.get("statut") != null)     d.setStatut((String) body.get("statut"));
        if (body.get("mention") != null)    d.setMention((String) body.get("mention"));
        if (body.get("photoUrl") != null)   d.setPhotoUrl((String) body.get("photoUrl"));
        if (body.get("dateInscription") != null) {
            java.time.LocalDate dateInscription = parseDate(body.get("dateInscription"));
            if (dateInscription != null) {
                d.setDateInscription(dateInscription);
            }
        }
        if (body.get("dateSoutenance") != null) {
            java.time.LocalDate dateSoutenance = parseDate(body.get("dateSoutenance"));
            if (dateSoutenance != null) {
                d.setDateSoutenance(dateSoutenance);
            }
        }
        if (body.containsKey("directeurId")) {
            Long directeurIdUpdate = parseLong(body.get("directeurId"));
            if (directeurIdUpdate != null)
                chercheurRepo.findById(directeurIdUpdate).ifPresent(d::setDirecteur);
            else d.setDirecteur(null);
        }
        Doctorant saved = doctorantRepo.save(d);
        auditService.log(request, "UPDATE", "Doctorant", id,
            "Doctorant modifié : " + saved.getPrenom() + " " + saved.getNom(), true);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<?> uploadPhoto(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) throws IOException {
        Optional<Doctorant> opt = doctorantRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Doctorant d = opt.get();
        String photoUrl = storageService.storePhoto(file, "profiles/doctorants");
        d.setPhotoUrl(photoUrl);
        doctorantRepo.save(d);
        auditService.log(request, "UPDATE", "Doctorant", id, "Photo doctorant mise à jour", true);
        return ResponseEntity.ok(Map.of("photoUrl", photoUrl));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        String nom = doctorantRepo.findById(id).map(d -> d.getPrenom() + " " + d.getNom()).orElse("id=" + id);
        doctorantRepo.deleteById(id);
        auditService.log(request, "DELETE", "Doctorant", id, "Doctorant supprimé : " + nom, true);
        return ResponseEntity.ok(Map.of("message", "Doctorant supprimé"));
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private java.time.LocalDate parseDate(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;
        try {
            return java.time.LocalDate.parse(text);
        } catch (Exception e) {
            return null;
        }
    }
}