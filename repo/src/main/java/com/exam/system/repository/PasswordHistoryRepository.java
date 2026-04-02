package com.exam.system.repository;

import com.exam.system.entity.PasswordHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    List<PasswordHistory> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}
