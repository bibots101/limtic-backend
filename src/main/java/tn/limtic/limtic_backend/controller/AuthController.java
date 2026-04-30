package tn.limtic.limtic_backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tn.limtic.limtic_backend.model.PasswordResetToken;
import tn.limtic.limtic_backend.model.User;
import tn.limtic.limtic_backend.repository.PasswordResetTokenRepository;
import tn.limtic.limtic_backend.repository.UserRepository;
import tn.limtic.limtic_backend.service.AuditService;
import tn.limtic.limtic_backend.service.SmtpSettingsService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final AuditService auditService;
    private final SmtpSettingsService smtpSettingsService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository,
                          PasswordResetTokenRepository resetTokenRepository,
                          AuditService auditService,
                          SmtpSettingsService smtpSettingsService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.auditService = auditService;
        this.smtpSettingsService = smtpSettingsService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        String email = body.get("email");
        String motDePasse = body.get("motDePasse");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Audit : tentative de login avec email inexistant
            auditService.log(request, "LOGIN", "User", null,
                "Tentative login échouée — email introuvable : " + email, false);
            return ResponseEntity.status(401).body(Map.of("error", "Email introuvable"));
        }

        User user = userOpt.get();
        if (!encoder.matches(motDePasse, user.getMotDePasse())) {
            // Audit : mauvais mot de passe
            auditService.log(request, "LOGIN", "User", user.getId(),
                "Tentative login échouée — mot de passe incorrect pour : " + email, false);
            return ResponseEntity.status(401).body(Map.of("error", "Mot de passe incorrect"));
        }

        if (!user.isActif()) {
            auditService.log(request, "LOGIN", "User", user.getId(),
                "Tentative login — compte désactivé : " + email, false);
            return ResponseEntity.status(403).body(Map.of("error", "Compte désactivé"));
        }

        // Créer la session Spring Security
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toString()))
            );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        );
        // Stocker l'email dans la session pour l'AuditService
        session.setAttribute("userEmail", user.getEmail());

        // Audit : connexion réussie
        auditService.log(request, "LOGIN", "User", user.getId(),
            "Connexion réussie : " + email + " [" + user.getRole() + "]", true);

        return ResponseEntity.ok(Map.of(
            "role", user.getRole().toString(),
            "email", user.getEmail(),
            "message", "Connexion réussie"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        auditService.log(request, "LOGOUT", "User", null, "Déconnexion", true);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Déconnecté avec succès"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
            auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of("error", "Non connecté"));
        }
        return ResponseEntity.ok(Map.of(
            "email", auth.getName(),
            "role", auth.getAuthorities().iterator().next()
                       .getAuthority().replace("ROLE_", "")
        ));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body,
                                     HttpServletRequest request) {
        String email = body.get("email");
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(400).body(Map.of("error", "Email déjà utilisé"));
        }
        User user = new User();
        user.setEmail(email);
        user.setMotDePasse(encoder.encode(body.get("motDePasse")));
        user.setRole(User.Role.VISITEUR);
        userRepository.save(user);

        auditService.log(request, "SIGNUP", "User", user.getId(),
            "Nouveau compte créé : " + email, true);

        return ResponseEntity.ok(Map.of("message", "Compte créé avec succès"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body,
                                             HttpServletRequest request) {
        String email = body.get("email");
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Si cet email existe, un lien a été envoyé."));
        }
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setExpiration(LocalDateTime.now().plusHours(1));
        resetTokenRepository.save(resetToken);

        try {
            var mailSender = smtpSettingsService.createMailSender();
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(email);
            mail.setSubject("Réinitialisation de votre mot de passe LIMTIC");
            mail.setText("Cliquez sur ce lien :\n\nhttps://localhost:4200/reset-password?token=" + token
                + "\n\nCe lien expire dans 1 heure.");
            mailSender.send(mail);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body(Map.of("error", "SMTP non configuré (DB/env)."));
        } catch (Exception e) {
            auditService.log(request, "FORGOT_PASSWORD_EMAIL_ECHEC", "User", userOpt.get().getId(),
                "Erreur envoi email reset: " + e.getMessage(), false);
            return ResponseEntity.status(500).body(Map.of("error", "Erreur lors de l'envoi de l'email."));
        }

        auditService.log(request, "FORGOT_PASSWORD", "User", userOpt.get().getId(),
            "Demande de réinitialisation mot de passe : " + email, true);

        return ResponseEntity.ok(Map.of("message", "Si cet email existe, un lien a été envoyé."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body,
                                            HttpServletRequest request) {
        String token = body.get("token");
        String newPassword = body.get("motDePasse");
        Optional<PasswordResetToken> tokenOpt = resetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().getExpiration().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body(Map.of("error", "Token invalide ou expiré"));
        }
        String email = tokenOpt.get().getEmail();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Utilisateur introuvable"));
        }
        User user = userOpt.get();
        user.setMotDePasse(encoder.encode(newPassword));
        userRepository.save(user);
        resetTokenRepository.delete(tokenOpt.get());

        auditService.log(request, "RESET_PASSWORD", "User", user.getId(),
            "Mot de passe réinitialisé : " + email, true);

        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
    }
}
