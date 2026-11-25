package com.example.expensetracker.transaction;

import com.example.expensetracker.category.CategoryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /** Always look up by id AND owner to enforce per-user isolation. */
    @EntityGraph(attributePaths = "category")
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    /** Category is fetched in the same query to avoid N+1 when mapping the page. */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    boolean existsByCategoryId(UUID categoryId);

    /** Sum of one type per category in a date range (used for budget "spent"). */
    @Query("""
            select t.category.id as categoryId, sum(t.amount) as total
            from Transaction t
            where t.user.id = :userId and t.type = :type
              and t.transactionDate between :from and :to
            group by t.category.id
            """)
    List<CategoryTotal> sumByCategory(@Param("userId") UUID userId, @Param("type") CategoryType type,
                                      @Param("from") LocalDate from, @Param("to") LocalDate to);

    interface CategoryTotal {
        UUID getCategoryId();

        BigDecimal getTotal();
    }
}
