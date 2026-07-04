package com.banking.forms.notification.infrastructure;

import com.banking.forms.notification.domain.NotificationMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, UUID> {

    List<NotificationMessage> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    List<NotificationMessage> findBySubmissionIdOrderByCreatedAtAsc(UUID submissionId);

    Optional<NotificationMessage> findByProviderCodeAndProviderMessageId(String providerCode, String providerMessageId);
}
