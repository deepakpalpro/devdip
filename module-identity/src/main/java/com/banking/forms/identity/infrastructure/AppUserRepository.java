package com.banking.forms.identity.infrastructure;

import com.banking.forms.identity.domain.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByTenantIdAndIdpSubject(UUID tenantId, String idpSubject);
}
