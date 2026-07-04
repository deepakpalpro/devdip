package com.banking.forms.notification.infrastructure;

import com.banking.forms.notification.domain.NotificationTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByEventTypeAndChannelTypeAndLocale(
            String eventType, String channelType, String locale);

    List<NotificationTemplate> findAllByOrderByEventTypeAscChannelTypeAscLocaleAsc();
}
