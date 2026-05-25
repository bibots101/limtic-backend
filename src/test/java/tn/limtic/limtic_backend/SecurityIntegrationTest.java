package tn.limtic.limtic_backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tn.limtic.limtic_backend.model.EmailVerificationToken;
import tn.limtic.limtic_backend.model.User;
import tn.limtic.limtic_backend.repository.EmailVerificationTokenRepository;
import tn.limtic.limtic_backend.repository.UserRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration — vérifient que la sécurité est bien configurée.
 * Utilise un profil "test" avec H2 en mémoire (voir application-test.properties).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository verificationRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ── Swagger protégé ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Swagger UI est inaccessible sans authentification (correction sécurité)")
    void swaggerUI_estProtege_sansAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isUnauthorized());
    }

    // ── Publications publiques ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/publications est accessible sans authentification")
    void publications_accessiblesPubliquement() throws Exception {
        mockMvc.perform(get("/api/publications"))
            .andExpect(status().isOk());
    }

    // ── Routes admin protégées ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/audit est refusé sans authentification")
    void auditLog_refuseSansAuth() throws Exception {
        mockMvc.perform(get("/api/admin/audit"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/chercheurs/export-csv est refusé sans auth")
    void exportCsv_refuseSansAuth() throws Exception {
        mockMvc.perform(get("/api/admin/chercheurs/export-csv"))
            .andExpect(status().isUnauthorized());
    }

    // ── Paramètres publics ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/parametres/public est accessible sans auth")
    void parametresPublics_accessiblesSansAuth() throws Exception {
        mockMvc.perform(get("/api/admin/parametres/public"))
            .andExpect(status().isOk());
    }

    // ── Login échoué ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login avec mauvais credentials retourne 401")
    void login_retourne401_siMauvaisCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"inexistant@test.tn\",\"motDePasse\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login autorise tous les comptes même si l'email n'est pas vérifié")
    void login_autorise_siEmailNonVerifieSansBlocage() throws Exception {
        userRepository.deleteAll();
        User user = new User();
        user.setEmail("pending@test.tn");
        user.setMotDePasse(encoder.encode("password123"));
        user.setRole(User.Role.ADMIN);
        user.setActif(true);
        user.setEmailVerified(false);
        user.setEmailVerificationRequired(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"pending@test.tn\",\"motDePasse\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.email").value("pending@test.tn"));
    }

    @Test
    @DisplayName("POST /api/auth/login autorise les comptes existants non vérifiés lorsque la vérification n'est pas requise")
    void login_autorise_siVerificationNonRequise() throws Exception {
        userRepository.deleteAll();
        User user = new User();
        user.setEmail("existing@test.tn");
        user.setMotDePasse(encoder.encode("password123"));
        user.setRole(User.Role.ADMIN);
        user.setActif(true);
        user.setEmailVerified(false);
        user.setEmailVerificationRequired(false);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"existing@test.tn\",\"motDePasse\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.email").value("existing@test.tn"));
    }

    @Test
    @DisplayName("POST /api/auth/verify-email avec token valide vérifie l'utilisateur")
    void verifyEmail_valide_marqueEmailCommeVerifie() throws Exception {
        userRepository.deleteAll();
        verificationRepository.deleteAll();

        User user = new User();
        user.setEmail("verify@test.tn");
        user.setMotDePasse(encoder.encode("password123"));
        user.setRole(User.Role.ADMIN);
        user.setActif(true);
        user.setEmailVerified(false);
        userRepository.save(user);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("test-token-123");
        token.setEmail(user.getEmail());
        token.setExpiration(LocalDateTime.now().plusHours(2));
        verificationRepository.save(token);

        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"test-token-123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Email vérifié avec succès"));

        User updatedUser = userRepository.findByEmail(user.getEmail()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(updatedUser.isEmailVerified());
        org.junit.jupiter.api.Assertions.assertTrue(verificationRepository.findByToken("test-token-123").isEmpty());
    }

    // ── Formulaire contact sans captcha ──────────────────────────────────────

    @Test
    @DisplayName("POST /api/contact sans captchaToken retourne 400")
    void contact_retourne400_sansCaptcha() throws Exception {
        mockMvc.perform(post("/api/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Test\",\"email\":\"test@test.tn\",\"message\":\"Hello\"}"))
            .andExpect(status().isBadRequest());
    }
}
