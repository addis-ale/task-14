package com.exam.system.security.rbac;

import com.exam.system.dto.ScopeDto;

public final class DataScopeHolder {

    private static final ThreadLocal<ScopeDto> HOLDER = new ThreadLocal<>();

    private DataScopeHolder() {
    }

    public static void set(ScopeDto scope) {
        HOLDER.set(scope);
    }

    public static ScopeDto get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
