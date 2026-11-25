package com.example.expensetracker.category;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /** System defaults + the user's own categories, optionally filtered by type. */
    @Query("""
            select c from Category c
            where (c.user is null or c.user.id = :userId)
              and (:type is null or c.type = :type)
            order by c.type, c.name
            """)
    List<Category> findVisibleToUser(@Param("userId") UUID userId, @Param("type") CategoryType type);

    /** Name clash check against defaults and the user's own categories (case-insensitive). */
    @Query("""
            select count(c) > 0 from Category c
            where (c.user is null or c.user.id = :userId)
              and c.type = :type
              and lower(c.name) = lower(:name)
              and (:excludeId is null or c.id <> :excludeId)
            """)
    boolean existsVisibleByName(@Param("userId") UUID userId, @Param("type") CategoryType type,
                                @Param("name") String name, @Param("excludeId") UUID excludeId);
}
