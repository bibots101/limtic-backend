package tn.limtic.limtic_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.limtic.limtic_backend.model.Doctorant;
import tn.limtic.limtic_backend.repository.DoctorantRepository;
import tn.limtic.limtic_backend.repository.ChercheurRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctorants")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class DoctorantController {

    private final DoctorantRepository doctorantRepo;
    private final ChercheurRepository chercheurRepo;

    public DoctorantController(DoctorantRepository doctorantRepo,
                                ChercheurRepository chercheurRepo) {
        this.doctorantRepo = doctorantRepo;
        this.chercheurRepo = chercheurRepo;
    }

    @GetMapping
    public List<Doctorant> getAll() {
        return doctorantRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return doctorantRepo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Doctorant d = new Doctorant();
        d.setNom((String) body.get("nom"));
        d.setPrenom((String) body.get("prenom"));
        d.setSujetThese((String) body.get("sujetThese"));
        d.setStatut((String) body.getOrDefault("statut", "EN_COURS"));
        d.setMention((String) body.get("mention"));
        d.setPhotoUrl((String) body.get("photoUrl"));
        if (body.get("dateInscription") != null)
            d.setDateInscription(java.time.LocalDate.parse(body.get("dateInscription").toString()));
        if (body.get("dateSoutenance") != null)
            d.setDateSoutenance(java.time.LocalDate.parse(body.get("dateSoutenance").toString()));
        if (body.get("directeurId") != null) {
            Long did = Long.valueOf(body.get("directeurId").toString());
            chercheurRepo.findById(did).ifPresent(d::setDirecteur);
        }
        return ResponseEntity.ok(doctorantRepo.save(d));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body) {
        Optional<Doctorant> opt = doctorantRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Doctorant d = opt.get();
        if (body.get("nom") != null)        d.setNom((String) body.get("nom"));
        if (body.get("prenom") != null)     d.setPrenom((String) body.get("prenom"));
        if (body.get("sujetThese") != null) d.setSujetThese((String) body.get("sujetThese"));
        if (body.get("statut") != null)     d.setStatut((String) body.get("statut"));
        if (body.get("mention") != null)    d.setMention((String) body.get("mention"));
        if (body.get("photoUrl") != null)   d.setPhotoUrl((String) body.get("photoUrl"));
        if (body.get("dateInscription") != null)
            d.setDateInscription(java.time.LocalDate.parse(body.get("dateInscription").toString()));
        if (body.get("dateSoutenance") != null)
            d.setDateSoutenance(java.time.LocalDate.parse(body.get("dateSoutenance").toString()));
        if (body.containsKey("directeurId")) {
            if (body.get("directeurId") != null) {
                Long did = Long.valueOf(body.get("directeurId").toString());
                chercheurRepo.findById(did).ifPresent(d::setDirecteur);
            } else {
                d.setDirecteur(null);
            }
        }
        return ResponseEntity.ok(doctorantRepo.save(d));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        doctorantRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Doctorant supprimé"));
    }
}