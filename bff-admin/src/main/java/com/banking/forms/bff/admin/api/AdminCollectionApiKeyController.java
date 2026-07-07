package com.banking.forms.bff.admin.api;

import com.banking.forms.collection.application.CollectionApiKeyService;
import com.banking.forms.collection.application.CollectionApiKeyService.CreatedApiKeyView;
import com.banking.forms.collection.application.CollectionApiKeyService.CollectionApiKeyView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/collection-api-keys")
public class AdminCollectionApiKeyController {

    private final CollectionApiKeyService apiKeyService;

    public AdminCollectionApiKeyController(CollectionApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public List<CollectionApiKeyView> list(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        return apiKeyService.listKeys(tenantId);
    }

    @PostMapping
    public ResponseEntity<CreatedApiKeyView> create(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @Valid @RequestBody CreateKeyRequest request) {
        CreatedApiKeyView created = apiKeyService.createKey(tenantId, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public void revoke(@RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable UUID id) {
        apiKeyService.revokeKey(tenantId, id);
    }

    public record CreateKeyRequest(@NotBlank String name) {}
}
