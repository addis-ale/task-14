package com.exam.system.repository;

import com.exam.system.entity.SysRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysRoleRepository extends JpaRepository<SysRole, Long> {

    Optional<SysRole> findByName(String name);

    List<SysRole> findByNameIn(List<String> names);
}
