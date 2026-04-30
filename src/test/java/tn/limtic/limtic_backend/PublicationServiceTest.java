package tn.limtic.limtic_backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.limtic.limtic_backend.model.Publication;
import tn.limtic.limtic_backend.repository.PublicationRepository;
import tn.limtic.limtic_backend.service.PublicationService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du PublicationService.
 * Utilise Mockito pour isoler le service de la base de données.
 */
@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {

    @Mock
    private PublicationRepository publicationRepository;

    @InjectMocks
    private PublicationService publicationService;

    private Publication pub1;
    private Publication pub2;

    @BeforeEach
    void setUp() {
        pub1 = new Publication();
        pub1.setId(1L);
        pub1.setTitre("Deep Learning for IoT Security");
        pub1.setType("Journal");
        pub1.setAnnee(2023);
        pub1.setJournal("IEEE Transactions on Neural Networks");
        pub1.setStatut("PUBLIE");
        pub1.setFacteurImpact(10.5);
        pub1.setScimagoQuartile("Q1");

        pub2 = new Publication();
        pub2.setId(2L);
        pub2.setTitre("NLP in Arabic Text Classification");
        pub2.setType("Conference");
        pub2.setAnnee(2022);
        pub2.setJournal("ACM Computing Surveys");
        pub2.setStatut("BROUILLON");
        pub2.setClassementCORE("A");
    }

    // ── getAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll() retourne la liste complète des publications")
    void getAll_retourneListeComplete() {
        when(publicationRepository.findAll()).thenReturn(List.of(pub1, pub2));

        List<Publication> result = publicationService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Publication::getTitre)
            .containsExactlyInAnyOrder(
                "Deep Learning for IoT Security",
                "NLP in Arabic Text Classification"
            );
        verify(publicationRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll() retourne une liste vide si aucune publication")
    void getAll_retourneListeVide() {
        when(publicationRepository.findAll()).thenReturn(List.of());

        List<Publication> result = publicationService.getAll();

        assertThat(result).isEmpty();
    }

    // ── getById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById() retourne la publication si elle existe")
    void getById_retournePublication_siExiste() {
        when(publicationRepository.findById(1L)).thenReturn(Optional.of(pub1));

        Publication result = publicationService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitre()).isEqualTo("Deep Learning for IoT Security");
        assertThat(result.getScimagoQuartile()).isEqualTo("Q1");
        assertThat(result.getFacteurImpact()).isEqualTo(10.5);
    }

    @Test
    @DisplayName("getById() retourne null si l'id n'existe pas")
    void getById_retourneNull_siInexistant() {
        when(publicationRepository.findById(99L)).thenReturn(Optional.empty());

        Publication result = publicationService.getById(99L);

        assertThat(result).isNull();
    }

    // ── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save() persiste la publication et retourne l'entité sauvegardée")
    void save_persistePublication() {
        when(publicationRepository.save(any(Publication.class))).thenReturn(pub1);

        Publication result = publicationService.save(pub1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(publicationRepository, times(1)).save(pub1);
    }

    // ── updateStatut ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatut() met à jour le statut de la publication")
    void updateStatut_metAJourLeStatut() {
        when(publicationRepository.findById(2L)).thenReturn(Optional.of(pub2));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        Publication result = publicationService.updateStatut(2L, "PUBLIE");

        assertThat(result.getStatut()).isEqualTo("PUBLIE");
        verify(publicationRepository, times(1)).save(pub2);
    }

    // ── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() appelle deleteById sur le repository")
    void delete_appelleRepository() {
        doNothing().when(publicationRepository).deleteById(1L);

        publicationService.delete(1L);

        verify(publicationRepository, times(1)).deleteById(1L);
    }

    // ── getByAnnee ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByAnnee() retourne les publications de l'année donnée")
    void getByAnnee_filtreParAnnee() {
        when(publicationRepository.findByAnnee(2023)).thenReturn(List.of(pub1));

        List<Publication> result = publicationService.getByAnnee(2023);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnnee()).isEqualTo(2023);
    }

    // ── Champs classement §3.7 ───────────────────────────────────────────────

    @Test
    @DisplayName("Les champs de classement sont correctement persistés (§3.7)")
    void save_persisteChampsDeclassement() {
        Publication pub = new Publication();
        pub.setTitre("Test IF");
        pub.setType("Journal");
        pub.setAnnee(2024);
        pub.setStatut("PUBLIE");
        pub.setFacteurImpact(5.2);
        pub.setScimagoQuartile("Q2");
        pub.setSnip(1.3);

        when(publicationRepository.save(any(Publication.class))).thenReturn(pub);

        Publication result = publicationService.save(pub);

        assertThat(result.getFacteurImpact()).isEqualTo(5.2);
        assertThat(result.getScimagoQuartile()).isEqualTo("Q2");
        assertThat(result.getSnip()).isEqualTo(1.3);
    }

    @Test
    @DisplayName("classementCORE est persisté pour les conférences")
    void save_persisteClassementCORE_pourConferences() {
        when(publicationRepository.save(any(Publication.class))).thenReturn(pub2);

        Publication result = publicationService.save(pub2);

        assertThat(result.getClassementCORE()).isEqualTo("A");
        assertThat(result.getType()).isEqualTo("Conference");
    }
}
