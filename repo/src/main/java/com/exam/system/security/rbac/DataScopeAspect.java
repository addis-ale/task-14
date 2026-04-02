package com.exam.system.security.rbac;

import com.exam.system.dto.ScopeDto;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Foundation aspect for ABAC row-level constraints.
 *
 * <p>The current implementation exposes scope context around repository access,
 * and is intended to be extended with entity-specific predicates as new modules
 * are added.</p>
 */
@Aspect
@Component
public class DataScopeAspect {

    private final EntityManager entityManager;

    public DataScopeAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Around("execution(* com.exam.system.repository..*(..))")
    public Object wrapRepositoryCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        UserContext context = UserContextHolder.get();
        if (context == null || "ADMIN".equalsIgnoreCase(context.getActiveRole())
                || "ACADEMIC_AFFAIRS".equalsIgnoreCase(context.getActiveRole())) {
            return joinPoint.proceed();
        }

        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("examSessionScope");
        ScopeDto scope = context.getScope();
        long termId = scope != null && scope.getTermId() != null ? scope.getTermId() : 0L;
        boolean gradeFilterEnabled = scope != null && scope.getGradeIds() != null && !scope.getGradeIds().isEmpty();
        java.util.List<Long> gradeIds = gradeFilterEnabled ? scope.getGradeIds() : java.util.List.of(-1L);
        filter.setParameter("termId", termId);
        filter.setParameter("gradeFilterEnabled", gradeFilterEnabled);
        filter.setParameterList("gradeIds", gradeIds);

        try {
            DataScopeHolder.set(context.getScope());
            return joinPoint.proceed();
        } finally {
            session.disableFilter("examSessionScope");
            DataScopeHolder.clear();
        }
    }
}
