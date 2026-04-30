package tn.limtic.limtic_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.limtic.limtic_backend.model.Chercheur;
import tn.limtic.limtic_backend.repository.ChercheurRepository;
import tn.limtic.limtic_backend.service.ChercheurService;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chercheurs")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class ChercheurController {

    private final ChercheurService chercheurService;
    private final ChercheurRepository chercheurRepository;

    public ChercheurController(ChercheurService chercheurService, ChercheurRepository chercheurRepository) {
        this.chercheurService = chercheurService;
        this.chercheurRepository = chercheurRepository;
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
    public Chercheur create(@RequestBody Chercheur chercheur) {
        return chercheurService.save(chercheur);
    }

    @PutMapping("/{id}")
    public Chercheur update(@PathVariable Long id, @RequestBody Chercheur chercheur) {
        chercheur.setId(id);
        return chercheurService.save(chercheur);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        chercheurService.delete(id);
    }

    @PatchMapping("/{id}/profil")
    public ResponseEntity<?> updateProfil(@PathVariable Long id, @RequestBody Map<String, String> body) {
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
        return ResponseEntity.ok(Map.of("message", "Profil mis à jour"));
    }
}