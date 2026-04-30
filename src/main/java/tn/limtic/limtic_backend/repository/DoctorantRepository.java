package tn.limtic.limtic_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.limtic.limtic_backend.model.Doctorant;

public interface DoctorantRepository extends JpaRepository<Doctorant, Long> {}