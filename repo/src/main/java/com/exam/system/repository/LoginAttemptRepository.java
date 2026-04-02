package com.exam.system.repository;

import com.exam.system.entity.LoginAttempt;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    List<LoginAttempt> findTop20ByUserIdAndAttemptedAtAfterOrderByAttemptedAtDesc(Long userId, LocalDateTime attemptedAfter);
}
