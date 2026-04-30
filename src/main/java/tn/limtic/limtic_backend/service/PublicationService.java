package tn.limtic.limtic_backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import tn.limtic.limtic_backend.model.Publication;
import tn.limtic.limtic_backend.repository.PublicationRepository;

@Service
public class PublicationService {

    private final PublicationRepository publicationRepository;

    public PublicationService(PublicationRepository publicationRepository) {
        this.publicationRepository = publicationRepository;
    }

    public List<Publication> getAll() {
        return publicationRepository.findAll();
    }

    public Publication getById(Long id) {
        return publicationRepository.findById(id).orElse(null);
    }

    public List<Publication> getByAnnee(int annee) {
        return publicationRepository.findByAnnee(annee);
    }

    public List<Publication> getByType(String type) {
        return publicationRepository.findByType(type);
    }

    public Publication save(Publication publication) {
        return publicationRepository.save(publication);
    }

    /**
     * Safe update: loads the existing entity and patches only the scalar /
     * user-editable fields.  Relations (axe, chercheurs) are intentionally
     * preserved from the DB so they are never accidentally nulled out by the
     * flat DTO that comes from the edit form.
     *
     * This is the method the PUT endpoint must call instead of save().
     */
    public Publication update(Long id, Publication incoming) {
        Publication existing = publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication introuvable : id=" + id));

        // ── Scalar fields ──────────────────────────────────────────────────
        existing.setTitre(incoming.getTitre());
        existing.setType(incoming.getType());
        existing.setAnnee(incoming.getAnnee());
        existing.setJournal(incoming.getJournal());
        existing.setResume(incoming.getResume());
        existing.setLienUrl(incoming.getLienUrl());
        existing.setDoi(incoming.getDoi());
        existing.setMotsCles(incoming.getMotsCles());
        existing.setStatut(incoming.getStatut() != null ? incoming.getStatut() : existing.getStatut());

        // ── §3.7 Classement / score ────────────────────────────────────────
        existing.setFacteurImpact(incoming.getFacteurImpact());
        existing.setScimagoQuartile(incoming.getScimagoQuartile());
        existing.setSnip(incoming.getSnip());
        existing.setClassementCORE(incoming.getClassementCORE());
        existing.setSourceClassement(incoming.getSourceClassement());

        // ── PDF URL: only overwrite when the caller explicitly provides one ─
        // (the upload-pdf endpoint manages pdfUrl separately; here we preserve
        //  the existing value unless the edit form explicitly clears it)
        if (incoming.getPdfUrl() != null) {
            existing.setPdfUrl(incoming.getPdfUrl());
        }
        // If incoming.getPdfUrl() == null we leave existing.pdfUrl untouched.
        // The DELETE /pdf endpoint handles explicit removal.

        // ── Relations: axe & chercheurs are NOT touched ────────────────────
        // The edit form does not send relation objects, so we must never
        // overwrite them here — that would break all axe/chercheur links.

        return publicationRepository.save(existing);
    }

    public void delete(Long id) {
        publicationRepository.deleteById(id);
    }

    public Publication updateStatut(Long id, String statut) {
        Publication pub = getById(id);
        pub.setStatut(statut);
        return publicationRepository.save(pub);
    }

    public Page<Publication> getPaged(Pageable pageable) {
        return publicationRepository.findAll(pageable);
    }
}