package com.exam.system.repository;

import com.exam.system.entity.UserScope;
import com.exam.system.entity.UserScopeId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserScopeRepository extends JpaRepository<UserScope, UserScopeId> {

    List<UserScope> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
