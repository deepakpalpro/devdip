package com.banking.forms.notification.infrastructure;

import com.banking.forms.notification.domain.NotificationProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationProviderRepository extends JpaRepository<NotificationProvider, UUID> {

    Optional<NotificationProvider> findByCode(String code);

    List<NotificationProvider> findAllByOrderByChannelTypeAscPriorityAsc();

    /** Enabled providers for a logical channel, best (lowest) priority first. */
    List<NotificationProvider> findByChannelTypeAndEnabledTrueOrderByPriorityAsc(String channelType);
}
