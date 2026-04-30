package tn.limtic.limtic_backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import tn.limtic.limtic_backend.service.AuditService;
import tn.limtic.limtic_backend.service.SmtpSettingsService;

/**
 * §3.9 CDC — Formulaire de contact avec captcha hCaptcha.
 *
 * Frontend Angular envoie :
 * {
 *   "nom": "...",
 *   "email": "...",
 *   "sujet": "...",
 *   "message": "...",
 *   "captchaToken": "<token renvoyé par hCaptcha widget>"
 * }
 *
 * Backend vérifie le token auprès de l'API hCaptcha avant d'envoyer l'email.
 *
 * Configuration requise dans application.properties :
 *   hcaptcha.secret=0x<votre_secret_hcaptcha>
 *   hcaptcha.verify-url=https://hcaptcha.com/siteverify
 */
@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class ContactController {

    private final AuditService auditService;
    private final SmtpSettingsService smtpSettingsService;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Secret hCaptcha — injecté depuis application.properties via variable d'env.
     * Valeur de test (toujours valide) : 0x0000000000000000000000000000000000000000
     * En production : remplacer par le vrai secret du compte hCaptcha.
     */
    @Value("${hcaptcha.secret:0x0000000000000000000000000000000000000000}")
    private String hcaptchaSecret;

    @Value("${hcaptcha.verify-url:https://hcaptcha.com/siteverify}")
    private String hcaptchaVerifyUrl;

    public ContactController(AuditService auditService, SmtpSettingsService smtpSettingsService) {
        this.auditService = auditService;
        this.smtpSettingsService = smtpSettingsService;
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> body,
                                          HttpServletRequest request) {

        String nom          = body.getOrDefault("nom", "").trim();
        String email        = body.getOrDefault("email", "").trim();
        String sujet        = body.getOrDefault("sujet", "").trim();
        String message      = body.getOrDefault("message", "").trim();
        String captchaToken = body.getOrDefault("captchaToken", "").trim();

        // ── Validation des champs ──────────────────────────────────────────
        if (nom.isEmpty() || email.isEmpty() || message.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Les champs nom, email et message sont obligatoires"));
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Adresse email invalide"));
        }
        if (message.length() > 5000) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Message trop long (max 5000 caractères)"));
        }

        // ── §3.9 Vérification captcha hCaptcha ────────────────────────────
        if (captchaToken.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Veuillez compléter le captcha"));
        }

        boolean captchaValide = verifierHcaptcha(captchaToken, getClientIp(request));
        if (!captchaValide) {
            auditService.log(request, "CONTACT_CAPTCHA_ECHEC", "Contact", null,
                "Captcha invalide depuis : " + email, false);
            return ResponseEntity.status(400)
                .body(Map.of("error", "Captcha invalide. Veuillez réessayer."));
        }

        // ── Envoi de l'email ───────────────────────────────────────────────
        try {
            var mailSender = smtpSettingsService.createMailSender();
            String destinataire = smtpSettingsService.getDestinataire();
            if (destinataire.isBlank()) {
                return ResponseEntity.status(500)
                    .body(Map.of("error", "SMTP destinataire manquant (DB/env)."));
            }

            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, "UTF-8");
            helper.setTo(destinataire);
            helper.setSubject("[LIMTIC Contact] " + (sujet.isEmpty() ? "Nouveau message" : sujet));
            helper.setText(
                "Nouveau message depuis le formulaire de contact LIMTIC\n\n" +
                "De : " + nom + " <" + email + ">\n" +
                "Sujet : " + sujet + "\n\n" +
                "Message :\n" + message,
                false
            );
            helper.setReplyTo(email);
            mailSender.send(mail);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "SMTP non configuré (DB/env)."));
        } catch (Exception e) {
            auditService.log(request, "CONTACT_EMAIL_ECHEC", "Contact", null,
                "Erreur envoi email depuis : " + email + " — " + e.getMessage(), false);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Erreur lors de l'envoi de l'email. Réessayez plus tard."));
        }

        auditService.log(request, "CONTACT", "Contact", null,
            "Message reçu de : " + nom + " <" + email + ">", true);

        return ResponseEntity.ok(Map.of("message", "Message envoyé avec succès"));
    }

    // ── Vérification hCaptcha ──────────────────────────────────────────────

    /**
     * Appelle l'API hCaptcha pour vérifier le token.
     * Retourne true si le token est valide.
     */
    @SuppressWarnings("unchecked")
    private boolean verifierHcaptcha(String token, String ip) {
        try {
            String url = hcaptchaVerifyUrl
                + "?secret=" + hcaptchaSecret
                + "&response=" + token
                + (ip != null && !ip.isEmpty() ? "&remoteip=" + ip : "");

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return false;
            return Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            // En cas d'erreur réseau vers hCaptcha, on refuse par sécurité
            return false;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
