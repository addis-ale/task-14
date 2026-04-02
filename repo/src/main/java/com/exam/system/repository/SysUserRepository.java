package com.exam.system.repository;

import com.exam.system.entity.SysUser;
import com.exam.system.entity.UserStatus;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysUserRepository extends JpaRepository<SysUser, Long>, JpaSpecificationExecutor<SysUser> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);

    long count(Specification<SysUser> spec);

    long countByStatus(UserStatus status);

    @Query("""
            select u from SysUser u join u.roles r
            where r.name = 'STUDENT' and u.status = com.exam.system.entity.UserStatus.ACTIVE
            """)
    List<SysUser> findActiveStudents();

    @Query("""
            select u from SysUser u
            where u.id in :userIds
            """)
    List<SysUser> findByIdInList(@Param("userIds") List<Long> userIds);
}
