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

    public Chercheur update(Long id, Chercheur updated) {
        Chercheur existing = chercheurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chercheur non trouve : " + id));
        if (updated.getNom() != null) existing.setNom(updated.getNom());
        if (updated.getPrenom() != null) existing.setPrenom(updated.getPrenom());
        if (updated.getGrade() != null) existing.setGrade(updated.getGrade());
        if (updated.getSpecialite() != null) existing.setSpecialite(updated.getSpecialite());
        if (updated.getBureau() != null) existing.setBureau(updated.getBureau());
        if (updated.getTelephone() != null) existing.setTelephone(updated.getTelephone());
        if (updated.getBiographie() != null) existing.setBiographie(updated.getBiographie());
        return chercheurRepository.save(existing);
    }
}
