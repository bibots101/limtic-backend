package tn.limtic.limtic_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.limtic.limtic_backend.model.EmailVerificationToken;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByEmail(String email);
}