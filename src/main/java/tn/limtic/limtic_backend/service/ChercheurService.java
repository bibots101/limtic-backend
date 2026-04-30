package tn.limtic.limtic_backend.service;

import org.springframework.stereotype.Service;
import tn.limtic.limtic_backend.model.Chercheur;
import tn.limtic.limtic_backend.repository.ChercheurRepository;
import java.util.List;

@Service
public class ChercheurService {

    private final ChercheurRepository chercheurRepository;

    public ChercheurService(ChercheurRepository chercheurRepository) {
        this.chercheurRepository = chercheurRepository;
    }

    public List<Chercheur> getAll() {
        return chercheurRepository.findAll();
    }

    public Chercheur getById(Long id) {
        return chercheurRepository.findById(id).orElse(null);
    }

    public Chercheur save(Chercheur chercheur) {
        return chercheurRepository.save(chercheur);
    }

    public void delete(Long id) {
        chercheurRepository.deleteById(id);
    }
}