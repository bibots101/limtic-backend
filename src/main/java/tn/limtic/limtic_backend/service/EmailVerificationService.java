package tn.limtic.limtic_backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import tn.limtic.limtic_backend.model.EmailVerificationToken;
import tn.limtic.limtic_backend.repository.EmailVerificationTokenRepository;

@Service
public class EmailVerificationService {

    private final SmtpSettingsService smtpSettingsService;
    private final EmailVerificationTokenRepository verificationRepository;
    private final AuditService auditService;
    private final FrontendUrlService frontendUrlService;

    public EmailVerificationService(SmtpSettingsService smtpSettingsService,
                                    EmailVerificationTokenRepository verificationRepository,
                                    AuditService auditService,
                                    FrontendUrlService frontendUrlService) {
        this.smtpSettingsService = smtpSettingsService;
        this.verificationRepository = verificationRepository;
        this.auditService = auditService;
        this.frontendUrlService = frontendUrlService;
    }

    public boolean sendVerificationEmail(String email, HttpServletRequest request) {
        String token = UUID.randomUUID().toString();
        EmailVerificationToken ev = new EmailVerificationToken();
        ev.setToken(token);
        ev.setEmail(email);
        ev.setExpiration(LocalDateTime.now().plusDays(2));
        verificationRepository.save(ev);

        String frontendUrl = frontendUrlService.resolveFrontendUrl(request);

        try {
            var mailSender = smtpSettingsService.createMailSender();
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(email);
            mail.setSubject("Vérifiez votre adresse email — LIMTIC");
            mail.setText("""
                Bonjour,

                Un compte a été créé pour vous sur LIMTIC. Veuillez vérifier votre adresse email en cliquant sur le lien suivant:

                %s/verify-email?token=%s

                Ce lien expire dans 48 heures.

                Si vous n'attendiez pas cet email, ignorez-le.
                """.formatted(frontendUrl, token));
            mailSender.send(mail);
            return true;
        } catch (IllegalStateException e) {
            auditService.log(request, "EMAIL_VERIF_CFG_MISSING", "User", null,
                "SMTP non configuré — verification non envoyée pour: " + email, false);
            return false;
        } catch (org.springframework.mail.MailException e) {
            auditService.log(request, "EMAIL_VERIF_SEND_ERR", "User", null,
                "Erreur envoi email verification: " + e.getMessage(), false);
            return false;
        }
    }

    @Transactional
    public boolean resendVerificationEmail(String email, HttpServletRequest request) {
        verificationRepository.deleteByEmail(email);
        verificationRepository.flush();
        return sendVerificationEmail(email, request);
    }
}
