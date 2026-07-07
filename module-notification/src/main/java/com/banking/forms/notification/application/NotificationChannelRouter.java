package com.banking.forms.notification.application;

import com.banking.forms.notification.domain.NotificationProvider;
import com.banking.forms.notification.infrastructure.NotificationProviderRepository;
import com.banking.forms.notification.spi.ChannelConfig;
import com.banking.forms.notification.spi.NotificationChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data-driven selection of the {@link NotificationChannel} that serves a logical channel. Mirrors the
 * form-import extractor router: for a channel type it picks the highest-priority enabled
 * {@code notification_provider} whose {@code code} matches an available channel bean.
 */
@Component
public class NotificationChannelRouter {

    private final NotificationProviderRepository providerRepository;
    private final Map<String, NotificationChannel> channelsByCode = new HashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationChannelRouter(
            NotificationProviderRepository providerRepository,
            List<NotificationChannel> channels,
            ObjectMapper objectMapper) {
        this.providerRepository = providerRepository;
        this.objectMapper = objectMapper;
        for (NotificationChannel channel : channels) {
            channelsByCode.put(channel.channelId(), channel);
        }
    }

    /** A selected provider row plus its bound implementation and parsed config. */
    public record Selection(String providerCode, NotificationChannel channel, ChannelConfig config) {}

    /** Highest-priority enabled provider for a logical channel that has an implementation. */
    @Transactional(readOnly = true)
    public Optional<Selection> resolve(String channelType) {
        return providerRepository.findByChannelTypeAndEnabledTrueOrderByPriorityAsc(channelType).stream()
                .filter(provider -> channelsByCode.containsKey(provider.getCode()))
                .findFirst()
                .map(this::toSelection);
    }

    /** Resolve a specific provider by code (used by the dispatcher for an already-queued message). */
    @Transactional(readOnly = true)
    public Optional<Selection> resolveProvider(String providerCode) {
        NotificationChannel channel = channelsByCode.get(providerCode);
        if (channel == null) {
            return Optional.empty();
        }
        return providerRepository.findByCode(providerCode).map(this::toSelection);
    }

    public boolean hasImplementation(String providerCode) {
        return channelsByCode.containsKey(providerCode);
    }

    private Selection toSelection(NotificationProvider provider) {
        return new Selection(provider.getCode(), channelsByCode.get(provider.getCode()), parseConfig(provider.getConfigJson()));
    }

    private ChannelConfig parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return new ChannelConfig(null);
        }
        try {
            return new ChannelConfig(objectMapper.readTree(json));
        } catch (Exception ex) {
            return new ChannelConfig(null);
        }
    }
}
