package tn.limtic.limtic_backend.service;

import org.springframework.stereotype.Service;
import tn.limtic.limtic_backend.model.Outil;
import tn.limtic.limtic_backend.repository.OutilRepository;
import java.util.List;

@Service
public class OutilService {

    private final OutilRepository outilRepository;

    public OutilService(OutilRepository outilRepository) {
        this.outilRepository = outilRepository;
    }

    public List<Outil> getAll() {
        return outilRepository.findAll();
    }

    public Outil getById(Long id) {
        return outilRepository.findById(id).orElse(null);
    }

    public Outil save(Outil outil) {
        return outilRepository.save(outil);
    }

    public void delete(Long id) {
        outilRepository.deleteById(id);
    }
}