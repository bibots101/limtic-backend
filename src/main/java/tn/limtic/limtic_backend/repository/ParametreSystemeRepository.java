package tn.limtic.limtic_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.limtic.limtic_backend.model.ParametreSysteme;

import java.util.List;
import java.util.Optional;

public interface ParametreSystemeRepository extends JpaRepository<ParametreSysteme, Long> {
    Optional<ParametreSysteme> findByCle(String cle);
    List<ParametreSysteme> findByGroupeOrderByCleAsc(String groupe);
}
