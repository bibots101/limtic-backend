package tn.limtic.limtic_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.limtic.limtic_backend.model.Evenement;
import tn.limtic.limtic_backend.model.PhotoEvenement;
import tn.limtic.limtic_backend.repository.EvenementRepository;
import tn.limtic.limtic_backend.service.EvenementService;
import tn.limtic.limtic_backend.service.AuditService;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/evenements")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class EvenementController {

    private final EvenementService evenementService;
    private final EvenementRepository evenementRepository;
    private final AuditService auditService;

    /** Répertoire de stockage des photos. Configurable dans application.properties */
    private final String uploadDir = "uploads/evenements/";

    public EvenementController(EvenementService evenementService,
                                EvenementRepository evenementRepository,
                                AuditService auditService) {
        this.evenementService = evenementService;
        this.evenementRepository = evenementRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Evenement> getAll() {
        return evenementService.getAll();
    }

    @GetMapping("/{id}")
    public Evenement getById(@PathVariable Long id) {
        return evenementService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Evenement> create(@RequestBody Evenement evenement,
                                             HttpServletRequest request) {
        Evenement saved = evenementService.save(evenement);
        auditService.log(request, "CREATE", "Evenement", saved.getId(),
            "Événement créé : " + saved.getTitre(), true);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Evenement> update(@PathVariable Long id,
                                             @RequestBody Evenement evenement,
                                             HttpServletRequest request) {
        evenement.setId(id);
        Evenement saved = evenementService.save(evenement);
        auditService.log(request, "UPDATE", "Evenement", id,
            "Événement modifié : " + saved.getTitre(), true);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        evenementService.delete(id);
        auditService.log(request, "DELETE", "Evenement", id, "Événement supprimé", true);
        return ResponseEntity.ok(Map.of("message", "Événement supprimé"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // GALERIE PHOTOS — Upload multiple (drag-drop côté frontend Angular)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * POST /api/evenements/{id}/photos
     * Accepte plusieurs fichiers image (multipart/form-data).
     * Renvoie la liste des objets PhotoEvenement créés.
     *
     * Exemple curl :
     *   curl -X POST -F "files=@photo1.jpg" -F "files=@photo2.jpg" \
     *        https://localhost:8443/api/evenements/1/photos
     */
    @PostMapping("/{id}/photos")
    public ResponseEntity<?> uploadPhotos(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request) {

        Evenement evenement = evenementService.getById(id);
        if (evenement == null) {
            return ResponseEntity.notFound().build();
        }

        // Créer le répertoire si inexistant
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Impossible de créer le dossier uploads"));
        }

        List<PhotoEvenement> savedPhotos = new ArrayList<>();

        for (MultipartFile file : files) {
            // Valider l'extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) continue;
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (!List.of("jpg", "jpeg", "png", "webp", "gif").contains(ext)) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Extension non autorisée : " + ext + ". Formats acceptés : jpg, jpeg, png, webp, gif")
                );
            }

            // Valider la taille (max 5 Mo)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Fichier trop lourd (max 5 Mo) : " + originalFilename)
                );
            }

            // Nom unique pour éviter les collisions
            String filename = "evt" + id + "_" + UUID.randomUUID() + "." + ext;
            Path destination = Paths.get(uploadDir + filename);

            try {
                Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                return ResponseEntity.status(500).body(Map.of("error", "Erreur sauvegarde fichier : " + originalFilename));
            }

            // Créer l'entité PhotoEvenement
            PhotoEvenement photo = new PhotoEvenement();
            photo.setUrl("/uploads/evenements/" + filename);
            photo.setLegende(originalFilename);
            photo.setOrdre(evenement.getPhotos().size() + savedPhotos.size());
            photo.setEvenement(evenement);
            evenement.getPhotos().add(photo);
            savedPhotos.add(photo);
        }

        evenementRepository.save(evenement);

        auditService.log(request, "UPLOAD_PHOTOS", "Evenement", id,
            savedPhotos.size() + " photo(s) ajoutée(s) à l'événement " + evenement.getTitre(), true);

        return ResponseEntity.ok(savedPhotos);
    }

    /**
     * DELETE /api/evenements/{evenementId}/photos/{photoId}
     * Supprime une photo de la galerie (et le fichier du disque).
     */
    @DeleteMapping("/{evenementId}/photos/{photoId}")
    public ResponseEntity<?> deletePhoto(
            @PathVariable Long evenementId,
            @PathVariable Long photoId,
            HttpServletRequest request) {

        Evenement evenement = evenementService.getById(evenementId);
        if (evenement == null) return ResponseEntity.notFound().build();

        evenement.getPhotos().removeIf(p -> {
            if (p.getId().equals(photoId)) {
                // Supprimer le fichier physique
                try {
                    String relativePath = p.getUrl().replace("/uploads/", "uploads/");
                    Files.deleteIfExists(Paths.get(relativePath));
                } catch (IOException ignored) {}
                return true;
            }
            return false;
        });

        evenementRepository.save(evenement);
        auditService.log(request, "DELETE_PHOTO", "Evenement", evenementId,
            "Photo " + photoId + " supprimée", true);

        return ResponseEntity.ok(Map.of("message", "Photo supprimée"));
    }

    /**
     * PATCH /api/evenements/{id}/photos/ordre
     * Met à jour l'ordre des photos (drag-drop).
     * Body : [ { "id": 1, "ordre": 0 }, { "id": 2, "ordre": 1 }, ... ]
     */
    @PatchMapping("/{id}/photos/ordre")
    public ResponseEntity<?> updateOrdre(
            @PathVariable Long id,
            @RequestBody List<Map<String, Integer>> ordres,
            HttpServletRequest request) {

        Evenement evenement = evenementService.getById(id);
        if (evenement == null) return ResponseEntity.notFound().build();

        for (Map<String, Integer> item : ordres) {
            Long photoId = item.get("id").longValue();
            int ordre = item.get("ordre");
            evenement.getPhotos().stream()
                .filter(p -> p.getId().equals(photoId))
                .findFirst()
                .ifPresent(p -> p.setOrdre(ordre));
        }

        evenementRepository.save(evenement);
        return ResponseEntity.ok(Map.of("message", "Ordre mis à jour"));
    }
}
