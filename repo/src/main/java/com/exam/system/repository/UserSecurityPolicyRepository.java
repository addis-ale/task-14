package com.exam.system.repository;

import com.exam.system.entity.UserSecurityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSecurityPolicyRepository extends JpaRepository<UserSecurityPolicy, Long> {
}
