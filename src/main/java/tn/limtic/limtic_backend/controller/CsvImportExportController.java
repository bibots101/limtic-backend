package tn.limtic.limtic_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.limtic.limtic_backend.model.Chercheur;
import tn.limtic.limtic_backend.repository.ChercheurRepository;
import tn.limtic.limtic_backend.service.AuditService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * §4.3.2 CDC — Import/Export CSV des membres du laboratoire.
 *
 * Export : GET  /api/admin/chercheurs/export-csv
 * Import : POST /api/admin/chercheurs/import-csv  (multipart/form-data, champ "file")
 *
 * Format CSV (séparateur virgule, première ligne = entêtes) :
 *   nom,prenom,grade,institution,specialite,bureau,telephone,orcid,googleScholar,researchGate,linkedin
 */
@RestController
@RequestMapping("/api/admin/chercheurs")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
public class CsvImportExportController {

    private static final String CSV_HEADER =
        "nom,prenom,grade,institution,specialite,bureau,telephone,orcid,googleScholar,researchGate,linkedin";

    private final ChercheurRepository chercheurRepository;
    private final AuditService auditService;

    public CsvImportExportController(ChercheurRepository chercheurRepository,
                                      AuditService auditService) {
        this.chercheurRepository = chercheurRepository;
        this.auditService = auditService;
    }

    // ── EXPORT ─────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/chercheurs/export-csv
     * Télécharge tous les chercheurs en CSV (UTF-8 BOM pour Excel).
     */
    @GetMapping("/export-csv")
    public void exportCsv(HttpServletResponse response,
                           HttpServletRequest request) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"chercheurs_limtic.csv\"");

        // BOM UTF-8 pour que Excel détecte l'encodage correctement
        OutputStream out = response.getOutputStream();
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.println(CSV_HEADER);

        List<Chercheur> chercheurs = chercheurRepository.findAll();
        for (Chercheur c : chercheurs) {
            writer.println(
                escapeField(c.getNom()) + "," +
                escapeField(c.getPrenom()) + "," +
                escapeField(c.getGrade()) + "," +
                escapeField(c.getInstitution()) + "," +
                escapeField(c.getSpecialite()) + "," +
                escapeField(c.getBureau()) + "," +
                escapeField(c.getTelephone()) + "," +
                escapeField(c.getOrcid()) + "," +
                escapeField(c.getGoogleScholar()) + "," +
                escapeField(c.getResearchGate()) + "," +
                escapeField(c.getLinkedin())
            );
        }
        writer.flush();

        auditService.log(request, "EXPORT_CSV", "Chercheur", null,
            "Export CSV de " + chercheurs.size() + " chercheurs", true);
    }

    // ── IMPORT ─────────────────────────────────────────────────────────────

    /**
     * POST /api/admin/chercheurs/import-csv
     * Importe des chercheurs depuis un fichier CSV.
     * - Les lignes avec un nom+prénom déjà existant sont IGNORÉES (pas de doublon).
     * - Les nouvelles lignes sont créées avec statut "ACTIF" et sans user associé.
     * - Retourne un rapport : { importes: N, ignores: N, erreurs: [...] }
     */
    @PostMapping("/import-csv")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file,
                                        HttpServletRequest request) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
        }
        if (!file.getOriginalFilename().endsWith(".csv")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le fichier doit être un .csv"));
        }

        int importes = 0;
        int ignores = 0;
        List<String> erreurs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String ligne;
            int numeroLigne = 0;

            while ((ligne = reader.readLine()) != null) {
                numeroLigne++;

                // Ignorer la ligne d'entête
                if (numeroLigne == 1) continue;

                // Ignorer les lignes vides
                if (ligne.trim().isEmpty()) continue;

                String[] cols = parseCsvLine(ligne);

                if (cols.length < 2) {
                    erreurs.add("Ligne " + numeroLigne + " : trop peu de colonnes");
                    continue;
                }

                String nom    = cols.length > 0 ? cols[0].trim() : "";
                String prenom = cols.length > 1 ? cols[1].trim() : "";

                if (nom.isEmpty() || prenom.isEmpty()) {
                    erreurs.add("Ligne " + numeroLigne + " : nom ou prénom manquant");
                    continue;
                }

                // Vérifier doublon (nom + prénom, insensible à la casse)
                boolean existe = chercheurRepository.findAll().stream()
                    .anyMatch(c -> c.getNom().equalsIgnoreCase(nom)
                               && c.getPrenom().equalsIgnoreCase(prenom));

                if (existe) {
                    ignores++;
                    continue;
                }

                // Créer le chercheur
                Chercheur c = new Chercheur();
                c.setNom(nom);
                c.setPrenom(prenom);
                if (cols.length > 2)  c.setGrade(cols[2].trim());
                if (cols.length > 3)  c.setInstitution(cols[3].trim());
                if (cols.length > 4)  c.setSpecialite(cols[4].trim());
                if (cols.length > 5)  c.setBureau(cols[5].trim());
                if (cols.length > 6)  c.setTelephone(cols[6].trim());
                if (cols.length > 7)  c.setOrcid(cols[7].trim());
                if (cols.length > 8)  c.setGoogleScholar(cols[8].trim());
                if (cols.length > 9)  c.setResearchGate(cols[9].trim());
                if (cols.length > 10) c.setLinkedin(cols[10].trim());

                chercheurRepository.save(c);
                importes++;
            }

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur lecture fichier : " + e.getMessage()));
        }

        auditService.log(request, "IMPORT_CSV", "Chercheur", null,
            "Import CSV : " + importes + " importés, " + ignores + " ignorés", true);

        return ResponseEntity.ok(Map.of(
            "importes", importes,
            "ignores", ignores,
            "erreurs", erreurs,
            "message", importes + " chercheur(s) importé(s) avec succès"
        ));
    }

    // ── Utilitaires ────────────────────────────────────────────────────────

    /** Échappe un champ CSV : encadre de guillemets si contient virgule ou guillemet */
    private String escapeField(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Parse une ligne CSV avec support des champs entre guillemets */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
