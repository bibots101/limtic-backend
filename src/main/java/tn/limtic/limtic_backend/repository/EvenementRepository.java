package tn.limtic.limtic_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.limtic.limtic_backend.model.Evenement;

public interface EvenementRepository extends JpaRepository<Evenement, Long> {
}