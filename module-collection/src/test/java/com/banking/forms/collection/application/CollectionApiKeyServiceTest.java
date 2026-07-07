package com.banking.forms.collection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.collection.domain.CollectionApiKey;
import com.banking.forms.collection.infrastructure.CollectionApiKeyRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectionApiKeyServiceTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private CollectionApiKeyRepository keyRepository;

    private CollectionApiKeyService service;

    @BeforeEach
    void setUp() {
        service = new CollectionApiKeyService(keyRepository);
    }

    @Test
    void authenticate_returnsTenantForValidKey() {
        String plain = "bfp_dev_collection_key";
        CollectionApiKey key = new CollectionApiKey(
                UUID.randomUUID(), TENANT, "dev", CollectionApiKeyService.hash(plain), "bfp_dev_coll");
        when(keyRepository.findByKeyHashAndEnabledTrue(CollectionApiKeyService.hash(plain)))
                .thenReturn(Optional.of(key));
        when(keyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.authenticate(plain)).contains(TENANT);
        verify(keyRepository).save(any(CollectionApiKey.class));
    }

    @Test
    void createKey_returnsPlainKeyOnce() {
        when(keyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CollectionApiKeyService.CreatedApiKeyView created = service.createKey(TENANT, "partner");

        assertThat(created.plainKey()).startsWith("bfp_");
        assertThat(created.key().name()).isEqualTo("partner");
        ArgumentCaptor<CollectionApiKey> captor = ArgumentCaptor.forClass(CollectionApiKey.class);
        verify(keyRepository).save(captor.capture());
        assertThat(captor.getValue().getKeyHash()).isEqualTo(CollectionApiKeyService.hash(created.plainKey()));
    }
}
