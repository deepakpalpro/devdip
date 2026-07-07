package com.banking.forms.collection.application;

import com.banking.forms.collection.domain.CollectionApiKey;
import com.banking.forms.collection.infrastructure.CollectionApiKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CollectionApiKeyService {

    private static final String KEY_PREFIX = "bfp_";
    private final CollectionApiKeyRepository keyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CollectionApiKeyService(CollectionApiKeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    @Transactional(readOnly = true)
    public List<CollectionApiKeyView> listKeys(UUID tenantId) {
        return keyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toView)
                .toList();
    }

    public CreatedApiKeyView createKey(UUID tenantId, String name) {
        String plainKey = KEY_PREFIX + randomToken();
        CollectionApiKey entity = keyRepository.save(new CollectionApiKey(
                UUID.randomUUID(),
                tenantId,
                name,
                hash(plainKey),
                plainKey.substring(0, Math.min(12, plainKey.length()))));
        return new CreatedApiKeyView(toView(entity), plainKey);
    }

    public void revokeKey(UUID tenantId, UUID keyId) {
        CollectionApiKey key = loadOwned(tenantId, keyId);
        key.revoke();
        keyRepository.save(key);
    }

    @Transactional
    public Optional<UUID> authenticate(String plainKey) {
        if (plainKey == null || plainKey.isBlank()) {
            return Optional.empty();
        }
        return keyRepository.findByKeyHashAndEnabledTrue(hash(plainKey)).map(key -> {
            key.markUsed(Instant.now());
            keyRepository.save(key);
            return key.getTenantId();
        });
    }

    private CollectionApiKey loadOwned(UUID tenantId, UUID keyId) {
        return keyRepository
                .findById(keyId)
                .filter(key -> key.getTenantId().equals(tenantId))
                .orElseThrow(() -> new CollectionException("Unknown collection API key: " + keyId));
    }

    private CollectionApiKeyView toView(CollectionApiKey key) {
        return new CollectionApiKeyView(
                key.getId(),
                key.getName(),
                key.getKeyPrefix(),
                key.isEnabled(),
                key.getCreatedAt(),
                key.getLastUsedAt());
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    static String hash(String plainKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(plainKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public record CollectionApiKeyView(
            UUID id,
            String name,
            String keyPrefix,
            boolean enabled,
            Instant createdAt,
            Instant lastUsedAt) {}

    public record CreatedApiKeyView(CollectionApiKeyView key, String plainKey) {}
}
