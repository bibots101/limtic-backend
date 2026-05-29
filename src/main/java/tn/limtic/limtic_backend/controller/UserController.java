package tn.limtic.limtic_backend.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import tn.limtic.limtic_backend.model.User;
import tn.limtic.limtic_backend.repository.EmailVerificationTokenRepository;
import tn.limtic.limtic_backend.repository.UserRepository;
import tn.limtic.limtic_backend.service.AuditService;
import tn.limtic.limtic_backend.service.EmailVerificationService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationTokenRepository verificationRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserController(UserRepository userRepository,
                          AuditService auditService,
                          EmailVerificationService emailVerificationService,
                          EmailVerificationTokenRepository verificationRepository) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.emailVerificationService = emailVerificationService;
        this.verificationRepository = verificationRepository;
    }

    // Liste tous les users
    @GetMapping
    public List<User> getAll() {
        return userRepository.findAll();
    }

    // Créer un user — Step 1: inactive until email verified
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body,
                                     HttpServletRequest request) {
        String email = body.get("email");
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(400).body(Map.of("error", "Email déjà utilisé"));
        }
        User user = new User();
        user.setEmail(email);
        user.setMotDePasse(encoder.encode(body.get("motDePasse")));
        user.setRole(User.Role.valueOf(body.get("role")));
        user.setActif(false);                          // locked until verified
        user.setEmailVerified(false);
        user.setEmailVerificationRequired(true);       // triggers the check at login
        userRepository.save(user);

        boolean emailSent = emailVerificationService.sendVerificationEmail(email, request);

        auditService.log(request, "CREATE", "User", user.getId(),
            "Compte créé (vérification requise) : " + email + " [" + user.getRole() + "]", true);

        return ResponseEntity.ok(Map.of(
            "message", emailSent
                ? "Compte créé. Un email de vérification a été envoyé à " + email + "."
                : "Compte créé, mais l'envoi de l'email a échoué (SMTP non configuré).",
            "emailSent", emailSent
        ));
    }

    // Changer le rôle
    @PatchMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id,
                                         @RequestBody Map<String, String> body,
                                         HttpServletRequest request) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User user = opt.get();
        String ancienRole = user.getRole().toString();
        user.setRole(User.Role.valueOf(body.get("role")));
        userRepository.save(user);
        auditService.log(request, "UPDATE", "User", id,
            "Rôle changé : " + user.getEmail() + " (" + ancienRole + " → " + user.getRole() + ")", true);
        return ResponseEntity.ok(Map.of("message", "Rôle mis à jour"));
    }

    // Activer / désactiver
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id, HttpServletRequest request) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User user = opt.get();
        user.setActif(!user.isActif());
        userRepository.save(user);
        String etat = user.isActif() ? "activé" : "désactivé";
        auditService.log(request, "UPDATE", "User", id,
            "Compte " + etat + " : " + user.getEmail(), true);
        return ResponseEntity.ok(Map.of("actif", user.isActif()));
    }

    // Step 3a — Force-activate: bypass email verification
    @PatchMapping("/{id}/force-activate")
    public ResponseEntity<?> forceActivate(@PathVariable Long id, HttpServletRequest request) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User user = opt.get();
        user.setEmailVerified(true);
        user.setEmailVerificationRequired(false);
        user.setActif(true);
        userRepository.save(user);

        try {
            // best effort: remove stale tokens, but never fail the activation itself
            verificationRepository.deleteByEmail(user.getEmail());
        } catch (Exception ignored) {
            // account is already activated; token cleanup can be retried later if needed
        }

        try {
            auditService.log(request, "FORCE_ACTIVATE", "User", id,
                "Compte activé de force (sans vérification email) : " + user.getEmail(), true);
        } catch (Exception ignored) {
            // auditing must not block the admin action
        }

        return ResponseEntity.ok(Map.of("message", "Compte activé sans vérification email."));
    }

    // Step 3b — Resend verification email
    @PostMapping("/{id}/resend-verification")
    public ResponseEntity<?> resendVerification(@PathVariable Long id, HttpServletRequest request) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User user = opt.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return ResponseEntity.status(400).body(Map.of("error", "Cet email est déjà vérifié."));
        }
        boolean emailSent = emailVerificationService.resendVerificationEmail(user.getEmail(), request);
        auditService.log(request, "RESEND_VERIFICATION", "User", id,
            "Renvoi email de vérification : " + user.getEmail() + " — envoyé: " + emailSent, true);
        return ResponseEntity.ok(Map.of(
            "message", emailSent
                ? "Email de vérification renvoyé à " + user.getEmail() + "."
                : "Échec de l'envoi (SMTP non configuré).",
            "emailSent", emailSent
        ));
    }

    // Supprimer
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Optional<User> opt = userRepository.findById(id);
        String email = opt.map(User::getEmail).orElse("id=" + id);
        userRepository.deleteById(id);
        auditService.log(request, "DELETE", "User", id,
            "Compte supprimé : " + email, true);
        return ResponseEntity.ok(Map.of("message", "Compte supprimé"));
    }
}