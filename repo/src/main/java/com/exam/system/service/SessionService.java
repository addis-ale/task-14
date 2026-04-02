package com.exam.system.service;

import com.exam.system.security.rbac.UserContext;
import java.util.Optional;

public interface SessionService {

    Optional<UserContext> resolve(String token);

    Optional<Long> resolveUserId(String token);
}
