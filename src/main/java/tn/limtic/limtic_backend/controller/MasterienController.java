package tn.limtic.limtic_backend.controller;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import tn.limtic.limtic_backend.model.Masterien;
import tn.limtic.limtic_backend.repository.ChercheurRepository;
import tn.limtic.limtic_backend.repository.MasterienRepository;
import tn.limtic.limtic_backend.service.AuditService;
import tn.limtic.limtic_backend.service.FileStorageService;

@RestController
@RequestMapping("/api/masteriens")
public class MasterienController {

    private final MasterienRepository masterienRepo;
    private final ChercheurRepository chercheurRepo;
    private final AuditService auditService;
    private final FileStorageService storageService;

    public MasterienController(MasterienRepository masterienRepo,
                                ChercheurRepository chercheurRepo,
                                AuditService auditService,
                                FileStorageService storageService) {
        this.masterienRepo = masterienRepo;
        this.chercheurRepo = chercheurRepo;
        this.auditService = auditService;
        this.storageService = storageService;
    }

    @GetMapping
    public List<Masterien> getAll() { return masterienRepo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return masterienRepo.findById(id).map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Masterien m = new Masterien();
        m.setNom((String) body.get("nom"));
        m.setPrenom((String) body.get("prenom"));
        m.setSujetMemoire((String) body.get("sujetMemoire"));
        m.setPromotion((String) body.get("promotion"));
        m.setStatut((String) body.getOrDefault("statut", "EN_COURS"));
        Long encadrantId = parseLong(body.get("encadrantId"));
        if (encadrantId != null)
            chercheurRepo.findById(encadrantId).ifPresent(m::setEncadrant);
        Masterien saved = masterienRepo.save(m);
        auditService.log(request, "CREATE", "Masterien", saved.getId(),
            "Mastérien créé : " + saved.getPrenom() + " " + saved.getNom(), true);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        Optional<Masterien> opt = masterienRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Masterien m = opt.get();
        if (body.get("nom") != null)          m.setNom((String) body.get("nom"));
        if (body.get("prenom") != null)       m.setPrenom((String) body.get("prenom"));
        if (body.get("sujetMemoire") != null) m.setSujetMemoire((String) body.get("sujetMemoire"));
        if (body.get("promotion") != null)    m.setPromotion((String) body.get("promotion"));
        if (body.get("statut") != null)       m.setStatut((String) body.get("statut"));
        if (body.containsKey("encadrantId")) {
            Long encadrantIdUpdate = parseLong(body.get("encadrantId"));
            if (encadrantIdUpdate != null)
                chercheurRepo.findById(encadrantIdUpdate).ifPresent(m::setEncadrant);
            else m.setEncadrant(null);
        }
        Masterien saved = masterienRepo.save(m);
        auditService.log(request, "UPDATE", "Masterien", id,
            "Mastérien modifié : " + saved.getPrenom() + " " + saved.getNom(), true);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<?> uploadPhoto(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) throws IOException {
        Optional<Masterien> opt = masterienRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Masterien m = opt.get();
        String photoUrl = storageService.storePhoto(file, "profiles/masteriens");
        m.setPhotoUrl(photoUrl);
        masterienRepo.save(m);
        auditService.log(request, "UPDATE", "Masterien", id, "Photo mastérien mise à jour", true);
        return ResponseEntity.ok(Map.of("photoUrl", photoUrl));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        String nom = masterienRepo.findById(id).map(m -> m.getPrenom() + " " + m.getNom()).orElse("id=" + id);
        masterienRepo.deleteById(id);
        auditService.log(request, "DELETE", "Masterien", id, "Mastérien supprimé : " + nom, true);
        return ResponseEntity.ok(Map.of("message", "Mastérien supprimé"));
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
}