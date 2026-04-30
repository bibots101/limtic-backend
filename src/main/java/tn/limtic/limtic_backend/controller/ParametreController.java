package tn.limtic.limtic_backend.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import tn.limtic.limtic_backend.model.ParametreSysteme;
import tn.limtic.limtic_backend.repository.ParametreSystemeRepository;
import tn.limtic.limtic_backend.service.AuditService;

/**
 * §4.3.6 CDC — API CRUD pour les paramètres généraux du laboratoire.
 *
 * GET  /api/admin/parametres              → tous les paramètres (valeurs sensibles masquées)
 * GET  /api/admin/parametres/public       → paramètres publics (labo, seo, contact) — sans auth
 * GET  /api/admin/parametres/{cle}        → un paramètre par sa clé
 * PUT  /api/admin/parametres/{cle}        → modifier la valeur d'un paramètre
 * PUT  /api/admin/parametres/lot          → modifier plusieurs paramètres en une requête
 * POST /api/admin/parametres              → créer un nouveau paramètre
 * POST /api/admin/parametres/logo         → uploader le logo du laboratoire (multipart)
 */
@RestController
@RequestMapping("/api/admin/parametres")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class ParametreController {

    private final ParametreSystemeRepository repo;
    private final AuditService auditService;

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    public ParametreController(ParametreSystemeRepository repo, AuditService auditService) {
        this.repo = repo;
        this.auditService = auditService;
    }

    // ── Endpoints publics ──────────────────────────────────────────────────────

    /**
     * Paramètres publics lisibles sans authentification.
     * Utilisés par le frontend pour afficher le nom du labo, le logo, les couleurs, etc.
     */
    @GetMapping("/public")
    public List<ParametreSysteme> getPublic() {
        List<ParametreSysteme> all = repo.findAll();
        all.forEach(p -> {
            if (p.isSensible()) p.setValeur("***");
        });
        return all.stream()
            .filter(p -> List.of("labo", "seo", "contact", "reseaux", "reseaux_sociaux", "theme").contains(p.getGroupe()))
            .toList();
    }

    // ── Endpoints admin ────────────────────────────────────────────────────────

    /** Liste tous les paramètres — masque les valeurs sensibles */
    @GetMapping
    public List<ParametreSysteme> getAll() {
        List<ParametreSysteme> all = repo.findAll();
        all.forEach(p -> {
            if (p.isSensible()) p.setValeur("***");
        });
        return all;
    }

    /** Récupère un paramètre par sa clé */
    @GetMapping("/{cle}")
    public ResponseEntity<ParametreSysteme> getByCle(@PathVariable String cle) {
        return repo.findByCle(cle)
            .map(p -> {
                if (p.isSensible()) p.setValeur("***");
                return ResponseEntity.ok(p);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /** Crée un nouveau paramètre */
    @PostMapping
    public ResponseEntity<ParametreSysteme> create(@RequestBody ParametreSysteme parametre,
                                                    HttpServletRequest request) {
        if (repo.findByCle(parametre.getCle()).isPresent()) {
            return ResponseEntity.status(409).build();
        }
        ParametreSysteme saved = repo.save(parametre);
        auditService.log(request, "CREATE", "ParametreSysteme", saved.getId(),
            "Paramètre créé : " + saved.getCle(), true);
        return ResponseEntity.ok(saved);
    }

    /**
     * §4.3.6 — Mise à jour en lot de plusieurs paramètres labo/thème en une seule requête.
     * Body : { "labo.nom": "LIMTIC", "theme.couleurPrimaire": "#00d2ff", ... }
     * Crée le paramètre s'il n'existe pas encore (upsert).
     *
     * IMPORTANT: Ce mapping doit rester AVANT @PutMapping("/{cle}") pour que Spring MVC
     * donne la priorité au chemin littéral "/lot" sur le paramètre de chemin "/{cle}".
     */
    @PutMapping("/lot")
    public ResponseEntity<Void> updateLot(@RequestBody Map<String, String> params,
                                           HttpServletRequest request) {
        params.forEach((cle, valeur) -> {
            if (valeur == null || "***".equals(valeur)) return;

            Optional<ParametreSysteme> opt = repo.findByCle(cle);
            ParametreSysteme p;
            if (opt.isPresent()) {
                p = opt.get();
                p.setValeur(valeur);
            } else {
                // Upsert : créer avec groupe déduit du préfixe de la clé
                p = new ParametreSysteme();
                p.setCle(cle);
                p.setValeur(valeur);
                p.setGroupe(cle.contains(".") ? cle.split("\\.")[0] : "labo");
                p.setSensible(false);
            }
            repo.save(p);
        });

        auditService.log(request, "UPDATE", "ParametreSysteme", null,
            "Mise à jour en lot : " + String.join(", ", params.keySet()), true);
        return ResponseEntity.ok().build();
    }

    /**
     * Modifie la valeur d'un paramètre par sa clé.
     * Body : { "valeur": "nouvelle valeur" }
     * Pour les valeurs sensibles, si le body contient "***", on ne modifie pas.
     */
    @PutMapping("/{cle:^(?!lot$).+}")
    public ResponseEntity<ParametreSysteme> update(@PathVariable String cle,
                                                    @RequestBody Map<String, String> body,
                                                    HttpServletRequest request) {
        Optional<ParametreSysteme> opt = repo.findByCle(cle);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        ParametreSysteme p = opt.get();
        String nouvelleValeur = body.get("valeur");

        if (!"***".equals(nouvelleValeur)) {
            p.setValeur(nouvelleValeur);
        }

        repo.save(p);
        auditService.log(request, "UPDATE", "ParametreSysteme", p.getId(),
            "Paramètre modifié : " + cle, true);

        if (p.isSensible()) p.setValeur("***");
        return ResponseEntity.ok(p);
    }

    /**
     * §4.3.6 — Upload du logo du laboratoire.
     * Enregistre le fichier dans uploads/logos/ et met à jour le paramètre labo.logoUrl.
     * Accepte : image/png, image/jpeg, image/svg+xml, image/webp (≤ 2 Mo).
     */
    @PostMapping("/logo")
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file,
                                                           HttpServletRequest request) {
        // Validation du type MIME
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Seules les images sont acceptées (PNG, JPEG, SVG, WebP)."));
        }

        // Validation de la taille (2 Mo max)
        if (file.getSize() > 2 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Le logo ne doit pas dépasser 2 Mo."));
        }

        try {
            // Créer le répertoire logos/ si absent
            Path logosDir = Paths.get(uploadDir).toAbsolutePath().resolve("logos");
            Files.createDirectories(logosDir);

            // Nom de fichier unique pour éviter les collisions et le cache navigateur
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "logo";
            String extension = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".png";
            String fileName = "logo-" + UUID.randomUUID().toString().substring(0, 8) + extension;

            Path destination = logosDir.resolve(fileName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            // URL relative servie par WebMvcConfig (/uploads/logos/xxx.png)
            String logoUrl = "/uploads/logos/" + fileName;

            // Upsert du paramètre labo.logoUrl
            ParametreSysteme p = repo.findByCle("labo.logoUrl").orElseGet(() -> {
                ParametreSysteme newP = new ParametreSysteme();
                newP.setCle("labo.logoUrl");
                newP.setGroupe("labo");
                newP.setDescription("URL du logo du laboratoire");
                newP.setSensible(false);
                return newP;
            });
            p.setValeur(logoUrl);
            repo.save(p);

            auditService.log(request, "UPDATE", "ParametreSysteme", p.getId(),
                "Logo mis à jour : " + logoUrl, true);

            return ResponseEntity.ok(Map.of("logoUrl", logoUrl));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Erreur lors de l'enregistrement du logo : " + e.getMessage()));
        }
    }
}