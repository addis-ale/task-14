package com.exam.system.service.impl.notification;

import com.exam.system.dto.notification.NotificationTargetScopeDto;
import com.exam.system.entity.SysRole;
import com.exam.system.entity.SysUser;
import com.exam.system.entity.UserScope;
import com.exam.system.entity.UserStatus;
import com.exam.system.repository.SysUserRepository;
import com.exam.system.repository.UserScopeRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NotificationTargetResolver {

    private final SysUserRepository userRepository;
    private final UserScopeRepository userScopeRepository;

    public NotificationTargetResolver(SysUserRepository userRepository, UserScopeRepository userScopeRepository) {
        this.userRepository = userRepository;
        this.userScopeRepository = userScopeRepository;
    }

    public Set<Long> resolveStudentRecipientIds(NotificationTargetScopeDto scope) {
        List<SysUser> students = userRepository.findActiveStudents();
        Set<Long> recipients = new HashSet<>();
        for (SysUser student : students) {
            if (student.getStatus() != UserStatus.ACTIVE) {
                continue;
            }
            boolean isStudent = student.getRoles().stream().map(SysRole::getName).anyMatch("STUDENT"::equalsIgnoreCase);
            if (!isStudent) {
                continue;
            }
            if (matchesScope(student.getId(), scope)) {
                recipients.add(student.getId());
            }
        }
        return recipients;
    }

    private boolean matchesScope(Long userId, NotificationTargetScopeDto scope) {
        if (scope == null || (scope.getGradeId() == null && scope.getSubjectId() == null)) {
            return true;
        }
        List<UserScope> userScopes = userScopeRepository.findByUserId(userId);
        for (UserScope userScope : userScopes) {
            boolean gradeMatch = scope.getGradeId() == null || scope.getGradeId().equals(userScope.getGradeId());
            boolean subjectMatch = scope.getSubjectId() == null || scope.getSubjectId().equals(userScope.getCourseId());
            if (gradeMatch && subjectMatch) {
                return true;
            }
        }
        return false;
    }
}
