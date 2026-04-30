package tn.limtic.limtic_backend.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import tn.limtic.limtic_backend.model.Publication;
import tn.limtic.limtic_backend.service.AuditService;
import tn.limtic.limtic_backend.service.PublicationService;

@RestController
@RequestMapping("/api/publications")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class PublicationController {

    private final PublicationService publicationService;
    private final AuditService auditService;

    private final String uploadDir =
        Paths.get(System.getProperty("user.dir"), "uploads", "publications").toAbsolutePath().normalize().toString();

    public PublicationController(PublicationService publicationService,
                                  AuditService auditService) {
        this.publicationService = publicationService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Publication> getAll() {
        return publicationService.getAll();
    }

    @GetMapping("/{id}")
    public Publication getById(@PathVariable Long id) {
        return publicationService.getById(id);
    }

    @GetMapping("/annee/{annee}")
    public List<Publication> getByAnnee(@PathVariable int annee) {
        return publicationService.getByAnnee(annee);
    }

    @GetMapping("/type/{type}")
    public List<Publication> getByType(@PathVariable String type) {
        return publicationService.getByType(type);
    }

    @PostMapping
    public ResponseEntity<Publication> create(@RequestBody Publication publication,
                                               HttpServletRequest request) {
        if (publication.getStatut() == null || publication.getStatut().isEmpty()) {
            publication.setStatut("BROUILLON");
        }
        Publication saved = publicationService.save(publication);
        auditService.log(request, "CREATE", "Publication", saved.getId(),
            "Publication créée : " + saved.getTitre(), true);
        return ResponseEntity.ok(saved);
    }

    /**
     * PUT /api/publications/{id}
     *
     * Uses publicationService.update() instead of save() so that existing
     * relations (axe, chercheurs) are preserved when the flat edit-form DTO
     * arrives without those fields.  This is the fix for the 500 error.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Publication> update(@PathVariable Long id,
                                               @RequestBody Publication publication,
                                               HttpServletRequest request) {
        try {
            Publication saved = publicationService.update(id, publication);
            auditService.log(request, "UPDATE", "Publication", id,
                "Publication modifiée : " + saved.getTitre(), true);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<Publication> updateStatut(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body,
                                                     HttpServletRequest request) {
        Publication updated = publicationService.updateStatut(id, body.get("statut"));
        auditService.log(request, "UPDATE_STATUT", "Publication", id,
            "Statut changé → " + body.get("statut"), true);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Publication pub = publicationService.getById(id);
        String titre = pub != null ? pub.getTitre() : "id=" + id;
        publicationService.delete(id);
        auditService.log(request, "DELETE", "Publication", id,
            "Publication supprimée : " + titre, true);
        return ResponseEntity.ok(Map.of("message", "Publication supprimée"));
    }

    @GetMapping("/page")
    public Page<Publication> getPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "annee") String sort,
        @RequestParam(defaultValue = "desc") String dir
    ) {
        Sort.Direction direction = dir.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        return publicationService.getPaged(pageable);
    }

    @PostMapping("/{id}/upload-pdf")
    public ResponseEntity<?> uploadPdf(@PathVariable Long id,
                                        @RequestParam("file") MultipartFile file,
                                        HttpServletRequest request) {
        if (file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));

        String ct = file.getContentType();
        if (ct == null || !ct.equals("application/pdf"))
            return ResponseEntity.badRequest().body(Map.of("error", "Seuls les fichiers PDF sont acceptés"));

        Publication pub = publicationService.getById(id);
        if (pub == null) return ResponseEntity.notFound().build();

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            String fileName = "pub_" + id + "_" + UUID.randomUUID() + ".pdf";
            Path dest = dir.resolve(fileName);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            pub.setPdfUrl("/api/publications/pdf/" + fileName);
            publicationService.save(pub);

            auditService.log(request, "UPLOAD_PDF", "Publication", id,
                "PDF uploadé : " + fileName, true);

            return ResponseEntity.ok(Map.of("pdfUrl", pub.getPdfUrl()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Erreur lors de l'upload : " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/pdf")
    public ResponseEntity<?> deletePdf(@PathVariable Long id, HttpServletRequest request) {
        Publication pub = publicationService.getById(id);
        if (pub == null) return ResponseEntity.notFound().build();

        String pdfUrl = pub.getPdfUrl();
        if (pdfUrl != null && !pdfUrl.isBlank()) {
            String filename = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1);
            try {
                Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log but don't fail — still clear the DB reference
            }
            pub.setPdfUrl(null);
            publicationService.save(pub);
            auditService.log(request, "DELETE_PDF", "Publication", id,
                "PDF supprimé pour publication id=" + id, true);
        }
        return ResponseEntity.ok(Map.of("message", "PDF supprimé"));
    }

    @GetMapping("/pdf/{filename:.+}")
    public ResponseEntity<Resource> servePdf(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .header("X-Frame-Options", "ALLOW-FROM https://localhost:4200")
                    .header("Content-Security-Policy",
                            "frame-ancestors 'self' https://localhost:4200 http://localhost:4200")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}