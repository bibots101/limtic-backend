package tn.limtic.limtic_backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.limtic.limtic_backend.model.AuditLog;
import tn.limtic.limtic_backend.service.AuditService;

/**
 * §4.1 CDC — Endpoint admin pour consulter le journal d'audit.
 * Accessible uniquement aux utilisateurs avec le rôle ADMIN.
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
     * Nécessite le rôle ADMIN (configuré dans SecurityConfig).
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<AuditLog>> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getAll(page, size));
    }
}
