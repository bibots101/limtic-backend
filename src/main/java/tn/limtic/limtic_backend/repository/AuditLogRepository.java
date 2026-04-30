package tn.limtic.limtic_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.limtic.limtic_backend.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByDateActionDesc(Pageable pageable);

    List<AuditLog> findByUserEmailContainingIgnoreCaseOrderByDateActionDesc(String email);

    List<AuditLog> findByActionOrderByDateActionDesc(String action);

    List<AuditLog> findByDateActionBetweenOrderByDateActionDesc(
        LocalDateTime debut, LocalDateTime fin
    );

    List<AuditLog> findByEntiteAndEntiteId(String entite, Long entiteId);
}
