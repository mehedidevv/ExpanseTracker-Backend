package com.example.expensetracker.budget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserIdAndYearAndMonth(UUID userId, Integer year, Integer month);

    @EntityGraph(attributePaths = "category")
    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByCategoryId(UUID categoryId);

    /** Uniqueness of (user, category, year, month); category null = overall budget. */
    @Query("""
            select count(b) > 0 from Budget b
            where b.user.id = :userId and b.year = :year and b.month = :month
              and ((:categoryId is null and b.category is null) or b.category.id = :categoryId)
              and (:excludeId is null or b.id <> :excludeId)
            """)
    boolean existsForPeriod(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId,
                            @Param("year") Integer year, @Param("month") Integer month,
                            @Param("excludeId") UUID excludeId);
}
