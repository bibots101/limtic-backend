package tn.limtic.limtic_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.limtic.limtic_backend.model.User;
import tn.limtic.limtic_backend.repository.UserRepository;
import tn.limtic.limtic_backend.service.AuditService;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class UserController {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserController(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // Liste tous les users
    @GetMapping
    public List<User> getAll() {
        return userRepository.findAll();
    }

    // Créer un user
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
        user.setActif(true);
        userRepository.save(user);
        auditService.log(request, "CREATE", "User", user.getId(),
            "Compte admin créé : " + email + " [" + user.getRole() + "]", true);
        return ResponseEntity.ok(Map.of("message", "Compte créé"));
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