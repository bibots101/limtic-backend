package tn.limtic.limtic_backend.service;

import org.springframework.stereotype.Service;
import tn.limtic.limtic_backend.model.Evenement;
import tn.limtic.limtic_backend.model.Intervenant;
import tn.limtic.limtic_backend.model.PhotoEvenement;
import tn.limtic.limtic_backend.repository.EvenementRepository;
import java.util.List;

@Service
public class EvenementService {

    private final EvenementRepository evenementRepository;

    public EvenementService(EvenementRepository evenementRepository) {
        this.evenementRepository = evenementRepository;
    }

    public List<Evenement> getAll() {
        return evenementRepository.findAll();
    }

    public Evenement getById(Long id) {
        return evenementRepository.findById(id).orElse(null);
    }

    public Evenement save(Evenement evenement) {
        if (evenement.getPhotos() != null) {
            for (PhotoEvenement photo : evenement.getPhotos()) {
                photo.setEvenement(evenement);
            }
        }
        if (evenement.getIntervenants() != null) {
            for (Intervenant intervenant : evenement.getIntervenants()) {
                intervenant.setEvenement(evenement);
            }
        }
        return evenementRepository.save(evenement);
    }

    public void delete(Long id) {
        evenementRepository.deleteById(id);
    }
}