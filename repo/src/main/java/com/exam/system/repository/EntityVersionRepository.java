package com.exam.system.repository;

import com.exam.system.entity.EntityVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntityVersionRepository extends JpaRepository<EntityVersion, Long> {

    Page<EntityVersion> findByEntityTypeAndEntityIdOrderByVersionNumberDesc(String entityType, Long entityId, Pageable pageable);

    EntityVersion findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(String entityType, Long entityId);
}
