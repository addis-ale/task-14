package com.exam.system.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter that encrypts Long values at rest using AES-256-GCM.
 * Applied to sensitive fields like student IDs.
 *
 * <p>Note: this converter should NOT be applied to columns used as foreign keys,
 * composite-key members, or in JPA/SQL JOINs (e.g. session_candidate.student_id),
 * because the encrypted representation would break relational lookups. Use it only
 * for display / audit fields that are never part of query predicates.</p>
 */
@Converter
@Component
public class EncryptedLongConverter implements AttributeConverter<Long, String> {

    private final AesCryptoService aesCryptoService;

    public EncryptedLongConverter(AesCryptoService aesCryptoService) {
        this.aesCryptoService = aesCryptoService;
    }

    @Override
    public String convertToDatabaseColumn(Long attribute) {
        if (attribute == null) {
            return null;
        }
        return aesCryptoService.encrypt(String.valueOf(attribute));
    }

    @Override
    public Long convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            String decrypted = aesCryptoService.decrypt(dbData);
            return Long.parseLong(decrypted);
        } catch (Exception e) {
            // Fallback: if data is not encrypted (legacy), parse directly
            try {
                return Long.parseLong(dbData);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }
}
