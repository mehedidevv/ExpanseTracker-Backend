package com.example.expensetracker.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    /** Newest code of this purpose (used or not), for the resend cooldown. */
    Optional<OtpCode> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, OtpPurpose purpose);

    /** Newest still-usable code of this purpose. */
    Optional<OtpCode> findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId, OtpPurpose purpose);

    @EntityGraph(attributePaths = "user")
    Optional<OtpCode> findByResetTokenHash(String resetTokenHash);

    /** Invalidates all open codes of this purpose (called before a new one is issued). */
    @Modifying
    @Query("update OtpCode o set o.usedAt = :now where o.user.id = :userId and o.purpose = :purpose and o.usedAt is null")
    int invalidateOpen(@Param("userId") UUID userId, @Param("purpose") OtpPurpose purpose, @Param("now") Instant now);

    @Modifying
    @Query("""
            delete from OtpCode o where o.expiresAt < :cutoff
            and (o.resetTokenExpiresAt is null or o.resetTokenExpiresAt < :cutoff)""")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
