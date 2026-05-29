package tn.limtic.limtic_backend.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import tn.limtic.limtic_backend.model.ParametreSysteme;
import tn.limtic.limtic_backend.repository.ParametreSystemeRepository;
import tn.limtic.limtic_backend.service.AuditService;
import tn.limtic.limtic_backend.service.FileStorageService;

@RestController
@RequestMapping("/api/directeur")
public class DirecteurController {

    private final ParametreSystemeRepository repo;
    private final AuditService auditService;
    private final FileStorageService storageService;

    private static final List<String> KEYS = List.of(
        "directeur.nom",
        "directeur.prenom",
        "directeur.titre",
        "directeur.specialite",
        "directeur.institution",
        "directeur.email",
        "directeur.telephone",
        "directeur.bureau",
        "directeur.photoUrl",
        "directeur.message",
        "directeur.googleScholarUrl",
        "directeur.researchgateUrl",
        "directeur.orcidUrl",
        "directeur.linkedinUrl"
    );

    public DirecteurController(ParametreSystemeRepository repo, AuditService auditService, FileStorageService storageService) {
        this.repo = repo;
        this.auditService = auditService;
        this.storageService = storageService;
    }

    @GetMapping
    public Map<String, String> getDirecteur() {
        Map<String, String> result = new HashMap<>();
        for (String key : KEYS) {
            Optional<ParametreSysteme> param = repo.findByCle(key);
            result.put(key.substring(key.indexOf('.') + 1), param.map(ParametreSysteme::getValeur).orElse(""));
        }
        return result;
    }

    @PutMapping
    public ResponseEntity<Void> updateDirecteur(@RequestBody Map<String, String> body,
                                                HttpServletRequest request) {
        for (String key : KEYS) {
            String shortKey = key.substring(key.indexOf('.') + 1);
            if (!body.containsKey(shortKey)) {
                continue;
            }

            String valeur = body.get(shortKey);
            Optional<ParametreSysteme> opt = repo.findByCle(key);
            ParametreSysteme param;
            if (opt.isPresent()) {
                param = opt.get();
            } else {
                param = new ParametreSysteme();
                param.setCle(key);
                param.setGroupe("directeur");
                param.setDescription("Paramètre de la page Directeur : " + shortKey);
                param.setSensible(false);
            }
            param.setValeur(valeur == null ? "" : valeur);
            repo.save(param);
        }

        auditService.log(request, "UPDATE", "Directeur", null, "Page directeur mise à jour", true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/photo")
    public ResponseEntity<?> uploadPhoto(@RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) throws IOException {
        String photoUrl = storageService.storePhoto(file, "profiles/directeur");

        Optional<ParametreSysteme> opt = repo.findByCle("directeur.photoUrl");
        ParametreSysteme param;
        if (opt.isPresent()) {
            param = opt.get();
        } else {
            param = new ParametreSysteme();
            param.setCle("directeur.photoUrl");
            param.setGroupe("directeur");
            param.setDescription("Paramètre de la page Directeur : photoUrl");
            param.setSensible(false);
        }
        param.setValeur(photoUrl);
        repo.save(param);
        auditService.log(request, "UPDATE", "Directeur", null, "Photo directeur mise à jour", true);
        return ResponseEntity.ok(Map.of("photoUrl", photoUrl));
    }
}
