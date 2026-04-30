package tn.limtic.limtic_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.limtic.limtic_backend.model.Publication;
import java.util.List;



public interface PublicationRepository extends JpaRepository<Publication, Long> {
    List<Publication> findByAnnee(int annee);
    List<Publication> findByType(String type);
    List<Publication> findByAxeId(Long axeId);
}