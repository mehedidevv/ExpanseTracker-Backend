package com.example.expensetracker.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, UUID> {

    Optional<PendingRegistration> findByEmail(String email);

    /** Deletes sign-ups whose last code was sent before the cutoff (never verified). */
    @Modifying
    @Query("delete from PendingRegistration p where p.lastSentAt < :cutoff")
    int deleteLastSentBefore(@Param("cutoff") Instant cutoff);
}
