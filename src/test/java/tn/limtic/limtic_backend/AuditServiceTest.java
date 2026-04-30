package tn.limtic.limtic_backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.limtic.limtic_backend.model.AuditLog;
import tn.limtic.limtic_backend.repository.AuditLogRepository;
import tn.limtic.limtic_backend.service.AuditService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de l'AuditService.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("log() enregistre l'action avec toutes les informations")
    void log_enregistreEntreeAudit() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userEmail")).thenReturn("admin@limtic.tn");
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(request, "CREATE", "Publication", 42L, "Publication créée", true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntite()).isEqualTo("Publication");
        assertThat(saved.getEntiteId()).isEqualTo(42L);
        assertThat(saved.getDetails()).isEqualTo("Publication créée");
        assertThat(saved.isSucces()).isTrue();
        assertThat(saved.getUserEmail()).isEqualTo("admin@limtic.tn");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("log() fonctionne sans session active (action anonyme)")
    void log_fonctionne_sansSestion() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getSession(false)).thenReturn(null);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(request, "CONTACT", "Contact", null, "Message reçu", true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getUserEmail()).isNull();
        assertThat(captor.getValue().getIpAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("log() extrait l'IP réelle depuis l'en-tête X-Forwarded-For (reverse proxy)")
    void log_extraitIpDepuisXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 198.51.100.1");
        when(request.getSession(false)).thenReturn(null);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(request, "LOGIN", "User", 1L, "Connexion", true);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        // Doit prendre la première IP (la vraie), pas le proxy
        assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("log() marque l'entrée comme échec quand succes=false")
    void log_marqueSuccesFalse() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(request.getSession(false)).thenReturn(null);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(request, "LOGIN", "User", null, "Login échoué", false);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().isSucces()).isFalse();
    }
}
