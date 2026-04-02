package com.exam.system.repository;

import com.exam.system.entity.NotifPreference;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifPreferenceRepository extends JpaRepository<NotifPreference, Long> {

    List<NotifPreference> findByUserId(Long userId);

    Optional<NotifPreference> findByUserIdAndEventType(Long userId, String eventType);
}
