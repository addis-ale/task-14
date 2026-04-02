package com.exam.system.service.impl;

import com.exam.system.dto.ScopeDto;
import com.exam.system.entity.UserScope;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ScopeAssembler {

    public ScopeDto toDto(List<UserScope> scopes) {
        ScopeDto dto = new ScopeDto();
        if (scopes == null || scopes.isEmpty()) {
            return dto;
        }

        dto.setGradeIds(scopes.stream()
                .map(UserScope::getGradeId)
                .filter(v -> v != null && v > 0)
                .distinct()
                .collect(Collectors.toList()));
        dto.setClassIds(scopes.stream()
                .map(UserScope::getClassId)
                .filter(v -> v != null && v > 0)
                .distinct()
                .collect(Collectors.toList()));
        dto.setCourseIds(scopes.stream()
                .map(UserScope::getCourseId)
                .filter(v -> v != null && v > 0)
                .distinct()
                .collect(Collectors.toList()));
        dto.setTermId(scopes.stream()
                .map(UserScope::getTermId)
                .filter(v -> v != null && v > 0)
                .findFirst()
                .orElse(null));
        return dto;
    }
}
