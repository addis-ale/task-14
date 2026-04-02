package com.exam.system.repository;

import com.exam.system.entity.SysSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysSessionRepository extends JpaRepository<SysSession, Long> {

    Optional<SysSession> findByToken(String token);

    List<SysSession> findByUserId(Long userId);

    void deleteByToken(String token);

    void deleteByExpiresAtBefore(LocalDateTime timestamp);
}
