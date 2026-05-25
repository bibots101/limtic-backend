package tn.limtic.limtic_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.limtic.limtic_backend.model.EmailVerificationToken;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
}
