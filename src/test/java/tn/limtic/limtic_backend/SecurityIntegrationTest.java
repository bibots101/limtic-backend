package tn.limtic.limtic_backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
