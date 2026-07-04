package com.banking.forms.downstream.application;

import com.banking.forms.downstream.domain.DownstreamProvider;
import com.banking.forms.downstream.infrastructure.DownstreamProviderRepository;
import com.banking.forms.downstream.spi.ConnectorConfig;
import com.banking.forms.downstream.spi.DownstreamConnector;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data-driven selection of {@link DownstreamConnector} implementations. Mirrors the notification
 * channel router: for dispatch it resolves a specific provider by code; for enqueue it returns all
 * enabled providers that have an available implementation bean.
 */
@Component
public class DownstreamConnectorRouter {

    private final DownstreamProviderRepository providerRepository;
    private final Map<String, DownstreamConnector> connectorsByCode = new HashMap<>();
    private final ObjectMapper objectMapper;

    public DownstreamConnectorRouter(
            DownstreamProviderRepository providerRepository,
            List<DownstreamConnector> connectors,
            ObjectMapper objectMapper) {
        this.providerRepository = providerRepository;
        this.objectMapper = objectMapper;
        for (DownstreamConnector connector : connectors) {
            connectorsByCode.put(connector.connectorId(), connector);
        }
    }

    /** A selected provider row plus its bound implementation and parsed config. */
    public record Selection(String providerCode, String connectorType, DownstreamConnector connector, ConnectorConfig config) {}

    /** Resolve a specific provider by code (used by the dispatcher for an already-queued outbox row). */
    @Transactional(readOnly = true)
    public Optional<Selection> resolveProvider(String providerCode) {
        DownstreamConnector connector = connectorsByCode.get(providerCode);
        if (connector == null) {
            return Optional.empty();
        }
        return providerRepository.findByCode(providerCode).map(this::toSelection);
    }

    /** All enabled providers that have an implementation — the fan-out set for a submission. */
    @Transactional(readOnly = true)
    public List<Selection> resolveAllEnabled() {
        return providerRepository.findByEnabledTrueOrderByPriorityAsc().stream()
                .filter(provider -> connectorsByCode.containsKey(provider.getCode()))
                .map(this::toSelection)
                .toList();
    }

    public boolean hasImplementation(String providerCode) {
        return connectorsByCode.containsKey(providerCode);
    }

    private Selection toSelection(DownstreamProvider provider) {
        return new Selection(
                provider.getCode(),
                provider.getConnectorType(),
                connectorsByCode.get(provider.getCode()),
                parseConfig(provider.getConfigJson()));
    }

    private ConnectorConfig parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return new ConnectorConfig(null);
        }
        try {
            return new ConnectorConfig(objectMapper.readTree(json));
        } catch (Exception ex) {
            return new ConnectorConfig(null);
        }
    }
}
