package com.banking.forms.discovery.infrastructure;

import com.banking.forms.discovery.domain.DiscoverySession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoverySessionRepository extends JpaRepository<DiscoverySession, UUID> {}
