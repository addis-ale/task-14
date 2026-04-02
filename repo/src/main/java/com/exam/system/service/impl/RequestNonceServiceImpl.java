package com.exam.system.service.impl;

import com.exam.system.entity.RequestNonce;
import com.exam.system.repository.RequestNonceRepository;
import com.exam.system.service.RequestNonceService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestNonceServiceImpl implements RequestNonceService {

    private final RequestNonceRepository requestNonceRepository;

    public RequestNonceServiceImpl(RequestNonceRepository requestNonceRepository) {
        this.requestNonceRepository = requestNonceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicate(String nonce) {
        return requestNonceRepository.findByNonce(nonce)
                .filter(stored -> stored.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Override
    @Transactional
    public void save(String nonce, int ttlSeconds) {
        RequestNonce entity = new RequestNonce();
        entity.setNonce(nonce);
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
        requestNonceRepository.save(entity);
    }

    @Override
    @Transactional
    public void cleanupExpired() {
        requestNonceRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
