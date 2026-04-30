package tn.limtic.limtic_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.limtic.limtic_backend.model.Chercheur;

public interface ChercheurRepository extends JpaRepository<Chercheur, Long> {
}