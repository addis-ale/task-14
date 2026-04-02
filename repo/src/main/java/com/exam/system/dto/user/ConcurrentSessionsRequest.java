package com.exam.system.dto.user;

public class ConcurrentSessionsRequest {

    private boolean allowed;

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }
}
