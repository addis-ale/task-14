package com.exam.system.repository;

import com.exam.system.entity.RequestNonce;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestNonceRepository extends JpaRepository<RequestNonce, Long> {

    Optional<RequestNonce> findByNonce(String nonce);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
