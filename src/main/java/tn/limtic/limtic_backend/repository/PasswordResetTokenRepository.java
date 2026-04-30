package tn.limtic.limtic_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.limtic.limtic_backend.model.PasswordResetToken;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
}