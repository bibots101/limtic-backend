package tn.limtic.limtic_backend.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import tn.limtic.limtic_backend.model.Chercheur;
import tn.limtic.limtic_backend.repository.ChercheurRepository;
import tn.limtic.limtic_backend.service.AuditService;
import tn.limtic.limtic_backend.service.ChercheurService;
import tn.limtic.limtic_backend.service.FileStorageService;

@RestController
@RequestMapping("/api/chercheurs")
public class ChercheurController {

    private final ChercheurService chercheurService;
    private final ChercheurRepository chercheurRepository;
    private final AuditService auditService;
    private final FileStorageService storageService;

    public ChercheurController(ChercheurService chercheurService,
                                ChercheurRepository chercheurRepository,
                                AuditService auditService,
                                FileStorageService storageService) {
        this.chercheurService = chercheurService;
        this.chercheurRepository = chercheurRepository;
        this.auditService = auditService;
        this.storageService = storageService;
    }

    @GetMapping
    public List<Chercheur> getAll() {
        return chercheurService.getAll();
    }

    @GetMapping("/{id}")
    public Chercheur getById(@PathVariable Long id) {
        return chercheurService.getById(id);
    }

    @PostMapping
    public Chercheur create(@RequestBody Chercheur chercheur, HttpServletRequest request) {
        Chercheur saved = chercheurService.save(chercheur);
        auditService.log(request, "CREATE", "Chercheur", saved.getId(),
            "Chercheur créé : " + saved.getPrenom() + " " + saved.getNom(), true);
        return saved;
    }

    @PutMapping("/{id}")
    public Chercheur update(@PathVariable Long id, @RequestBody Chercheur chercheur,
                             HttpServletRequest request) {
        chercheur.setId(id);
        Chercheur saved = chercheurService.save(chercheur);
        auditService.log(request, "UPDATE", "Chercheur", id,
            "Chercheur modifié : " + saved.getPrenom() + " " + saved.getNom(), true);
        return saved;
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<?> uploadPhoto(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) throws IOException {
        Optional<Chercheur> opt = chercheurRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Chercheur c = opt.get();
        String photoUrl = storageService.storePhoto(file, "profiles/chercheurs");
        c.setPhotoUrl(photoUrl);
        chercheurRepository.save(c);
        auditService.log(request, "UPDATE", "Chercheur", id, "Photo chercheur mise à jour", true);
        return ResponseEntity.ok(Map.of("photoUrl", photoUrl));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Chercheur c = chercheurService.getById(id);
        String nom = (c != null) ? c.getPrenom() + " " + c.getNom() : "id=" + id;
        chercheurService.delete(id);
        auditService.log(request, "DELETE", "Chercheur", id,
            "Chercheur supprimé : " + nom, true);
        return ResponseEntity.ok(Map.of("message", "Chercheur supprimé"));
    }

    @PatchMapping("/{id}/profil")
    public ResponseEntity<?> updateProfil(@PathVariable Long id,
                                           @RequestBody Map<String, String> body,
                                           HttpServletRequest request) {
        Optional<Chercheur> opt = chercheurRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Chercheur c = opt.get();
        if (body.containsKey("grade"))         c.setGrade(body.get("grade"));
        if (body.containsKey("specialite"))    c.setSpecialite(body.get("specialite"));
        if (body.containsKey("institution"))   c.setInstitution(body.get("institution"));
        if (body.containsKey("bureau"))        c.setBureau(body.get("bureau"));
        if (body.containsKey("telephone"))     c.setTelephone(body.get("telephone"));
        if (body.containsKey("biographie"))    c.setBiographie(body.get("biographie"));
        if (body.containsKey("googleScholar")) c.setGoogleScholar(body.get("googleScholar"));
        if (body.containsKey("researchGate"))  c.setResearchGate(body.get("researchGate"));
        if (body.containsKey("orcid"))         c.setOrcid(body.get("orcid"));
        if (body.containsKey("linkedin"))      c.setLinkedin(body.get("linkedin"));
        chercheurRepository.save(c);
        auditService.log(request, "UPDATE", "Chercheur", id,
            "Profil mis à jour : " + c.getPrenom() + " " + c.getNom(), true);
        return ResponseEntity.ok(Map.of("message", "Profil mis à jour"));
    }
}