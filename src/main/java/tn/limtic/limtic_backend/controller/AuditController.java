package tn.limtic.limtic_backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.limtic.limtic_backend.model.AuditLog;
import tn.limtic.limtic_backend.service.AuditService;

/**
 * §4.1 CDC — Endpoint admin pour consulter le journal d'audit.
 * Accessible aux utilisateurs avec le rôle ADMIN ou SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/audit")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * GET /api/admin/audit?page=0&size=50
     * Retourne les dernières entrées du journal, paginées.
     * Accessible aux rôles ADMIN et SUPER_ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'CHERCHEUR')")
    public ResponseEntity<Page<AuditLog>> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(auditService.getAll(page, size));
    }
}
