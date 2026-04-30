package tn.limtic.limtic_backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tn.limtic.limtic_backend.model.AuditLog;
import tn.limtic.limtic_backend.repository.AuditLogRepository;

import java.time.LocalDateTime;

/**
 * Service centralisé pour écrire dans le journal d'audit.
 *
 * Usage depuis n'importe quel Controller ou Service :
 *   auditService.log(request, "DELETE", "Publication", 42L, "Publication supprimée : xxx", true);
 */
@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    /**
     * Enregistre une entrée dans le journal d'audit.
     *
     * @param request   La requête HTTP (pour extraire IP et user de session)
     * @param action    Type d'action (LOGIN, CREATE, UPDATE, DELETE, EXPORT...)
     * @param entite    Entité concernée (Publication, Chercheur...)
     * @param entiteId  ID de l'entité (peut être null)
     * @param details   Description libre de l'action
     * @param succes    true si l'action a réussi, false sinon
     */
    public void log(HttpServletRequest request,
                    String action,
                    String entite,
                    Long entiteId,
                    String details,
                    boolean succes) {

        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntite(entite);
        log.setEntiteId(entiteId);
        log.setDetails(details);
        log.setSucces(succes);
        log.setDateAction(LocalDateTime.now());
        log.setIpAddress(getClientIp(request));

        // Récupérer l'email de l'utilisateur connecté depuis la session
        if (request.getSession(false) != null) {
            Object emailAttr = request.getSession(false).getAttribute("userEmail");
            if (emailAttr != null) {
                log.setUserEmail(emailAttr.toString());
            }
        }

        repo.save(log);
    }

    /** Surcharge sans entiteId ni request (pour les actions anonymes simples) */
    public void log(String action, String entite, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntite(entite);
        log.setDetails(details);
        log.setDateAction(LocalDateTime.now());
        repo.save(log);
    }

    public Page<AuditLog> getAll(int page, int size) {
        return repo.findAllByOrderByDateActionDesc(PageRequest.of(page, size));
    }

    // Extrait l'IP réelle même derrière un reverse proxy
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
