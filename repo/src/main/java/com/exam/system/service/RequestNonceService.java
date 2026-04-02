package com.exam.system.service;

public interface RequestNonceService {

    boolean isDuplicate(String nonce);

    void save(String nonce, int ttlSeconds);

    void cleanupExpired();
}
