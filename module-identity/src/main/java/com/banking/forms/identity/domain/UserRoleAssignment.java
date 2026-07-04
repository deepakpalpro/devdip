package com.banking.forms.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "user_role")
@IdClass(UserRoleAssignment.UserRoleId.class)
public class UserRoleAssignment {

    @Id
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    protected UserRoleAssignment() {}

    public UserRoleAssignment(UUID userId, UserRole role) {
        this.userId = userId;
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    public static class UserRoleId implements Serializable {
        private UUID userId;
        private UserRole role;

        public UserRoleId() {}

        public UserRoleId(UUID userId, UserRole role) {
            this.userId = userId;
            this.role = role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserRoleId that)) return false;
            return userId.equals(that.userId) && role == that.role;
        }

        @Override
        public int hashCode() {
            return userId.hashCode() * 31 + role.hashCode();
        }
    }
}
