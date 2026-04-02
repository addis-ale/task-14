package com.exam.system.repository;

import com.exam.system.entity.AutoSaveDraft;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoSaveDraftRepository extends JpaRepository<AutoSaveDraft, Long> {

    Optional<AutoSaveDraft> findByUserIdAndFormKey(Long userId, String formKey);

    List<AutoSaveDraft> findByUserIdAndSavedAtAfter(Long userId, LocalDateTime threshold);

    void deleteByUserIdAndFormKey(Long userId, String formKey);

    void deleteBySavedAtBefore(LocalDateTime threshold);
}
