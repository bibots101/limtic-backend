package tn.limtic.limtic_backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import tn.limtic.limtic_backend.model.EmailVerificationToken;
import tn.limtic.limtic_backend.repository.EmailVerificationTokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private final SmtpSettingsService smtpSettingsService;
    private final EmailVerificationTokenRepository verificationRepository;
    private final AuditService auditService;

    public EmailVerificationService(SmtpSettingsService smtpSettingsService,
                                    EmailVerificationTokenRepository verificationRepository,
                                    AuditService auditService) {
        this.smtpSettingsService = smtpSettingsService;
        this.verificationRepository = verificationRepository;
        this.auditService = auditService;
    }

    public boolean sendVerificationEmail(String email, HttpServletRequest request) {
        String token = UUID.randomUUID().toString();
        EmailVerificationToken ev = new EmailVerificationToken();
        ev.setToken(token);
        ev.setEmail(email);
        ev.setExpiration(LocalDateTime.now().plusDays(2));
        verificationRepository.save(ev);

        try {
            var mailSender = smtpSettingsService.createMailSender();
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(email);
            mail.setSubject("Vérifiez votre adresse email — LIMTIC");
            mail.setText("Bonjour,\n\nUn compte a été créé pour vous sur LIMTIC. Veuillez vérifier votre adresse email en cliquant sur le lien suivant:\n\n"
                + "https://localhost:4200/verify-email?token=" + token
                + "\n\nCe lien expire dans 48 heures.\n\nSi vous n'attendiez pas cet email, ignorez-le.");
            mailSender.send(mail);
            return true;
        } catch (IllegalStateException e) {
            auditService.log(request, "EMAIL_VERIF_CFG_MISSING", "User", null,
                "SMTP non configuré — verification non envoyée pour: " + email, false);
            return false;
        } catch (Exception e) {
            auditService.log(request, "EMAIL_VERIF_SEND_ERR", "User", null,
                "Erreur envoi email verification: " + e.getMessage(), false);
            return false;
        }
    }
}
