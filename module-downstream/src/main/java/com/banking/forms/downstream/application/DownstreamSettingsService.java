package com.banking.forms.downstream.application;

import com.banking.forms.downstream.domain.DownstreamProvider;
import com.banking.forms.downstream.domain.OutboxEvent;
import com.banking.forms.downstream.infrastructure.DownstreamProviderRepository;
import com.banking.forms.downstream.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read/update access to configurable downstream providers and the outbox delivery log surfaced in
 * the admin Settings UI.
 */
@Service
@Transactional
public class DownstreamSettingsService {

    private final DownstreamProviderRepository providerRepository;
    private final OutboxEventRepository outboxRepository;
    private final DownstreamConnectorRouter connectorRouter;
    private final ObjectMapper objectMapper;

    public DownstreamSettingsService(
            DownstreamProviderRepository providerRepository,
            OutboxEventRepository outboxRepository,
            DownstreamConnectorRouter connectorRouter,
            ObjectMapper objectMapper) {
        this.providerRepository = providerRepository;
        this.outboxRepository = outboxRepository;
        this.connectorRouter = connectorRouter;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<DownstreamProviderView> listProviders() {
        return providerRepository.findAllByOrderByConnectorTypeAscPriorityAsc().stream()
                .map(this::toView)
                .toList();
    }

    public DownstreamProviderView updateProvider(String code, boolean enabled, int priority, JsonNode config) {
        DownstreamProvider provider = providerRepository
                .findByCode(code)
                .orElseThrow(() -> new DownstreamException("Unknown downstream provider: " + code));
        provider.update(enabled, priority, writeConfig(config));
        providerRepository.save(provider);
        return toView(provider);
    }

    @Transactional(readOnly = true)
    public List<OutboxEventView> listOutboxForSubmission(UUID submissionId) {
        return outboxRepository.findBySubmissionIdOrderByCreatedAtAsc(submissionId).stream()
                .map(this::toOutboxView)
                .toList();
    }

    private DownstreamProviderView toView(DownstreamProvider provider) {
        return new DownstreamProviderView(
                provider.getCode(),
                provider.getName(),
                provider.getConnectorType(),
                provider.isEnabled(),
                provider.getPriority(),
                connectorRouter.hasImplementation(provider.getCode()),
                readConfig(provider.getConfigJson()));
    }

    private OutboxEventView toOutboxView(OutboxEvent event) {
        return new OutboxEventView(
                event.getId(),
                event.getSubmissionId(),
                event.getEventType(),
                event.getFormCode(),
                event.getProviderCode(),
                event.getConnectorType(),
                event.getStatus().name(),
                event.getAttempts(),
                event.getProviderRef(),
                event.getError(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    private JsonNode readConfig(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    private String writeConfig(JsonNode config) {
        if (config == null || config.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception ex) {
            throw new DownstreamException("Invalid provider config JSON");
        }
    }
}
