package com.example.expensetracker.report;

import com.example.expensetracker.category.CategoryType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Read-only aggregate queries over transactions. No entity of its own. */
@Repository
public class ReportRepository {

    @PersistenceContext
    private EntityManager em;

    public record TypeTotal(CategoryType type, BigDecimal total) {
    }

    public record CategoryTotal(UUID categoryId, String categoryName, BigDecimal total) {
    }

    public record MonthTypeTotal(int month, CategoryType type, BigDecimal total) {
    }

    public List<TypeTotal> totalsByType(UUID userId, LocalDate from, LocalDate to) {
        return em.createQuery("""
                        select new com.example.expensetracker.report.ReportRepository$TypeTotal(t.type, sum(t.amount))
                        from Transaction t
                        where t.user.id = :userId and t.transactionDate between :from and :to
                        group by t.type
                        """, TypeTotal.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public List<CategoryTotal> expenseByCategory(UUID userId, LocalDate from, LocalDate to) {
        return em.createQuery("""
                        select new com.example.expensetracker.report.ReportRepository$CategoryTotal(
                                c.id, c.name, sum(t.amount))
                        from Transaction t join t.category c
                        where t.user.id = :userId and t.type = :type
                          and t.transactionDate between :from and :to
                        group by c.id, c.name
                        order by sum(t.amount) desc, c.name
                        """, CategoryTotal.class)
                .setParameter("userId", userId)
                .setParameter("type", CategoryType.EXPENSE)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public List<MonthTypeTotal> monthlyTotals(UUID userId, int year) {
        return em.createQuery("""
                        select new com.example.expensetracker.report.ReportRepository$MonthTypeTotal(
                                extract(month from t.transactionDate), t.type, sum(t.amount))
                        from Transaction t
                        where t.user.id = :userId and t.transactionDate between :from and :to
                        group by extract(month from t.transactionDate), t.type
                        """, MonthTypeTotal.class)
                .setParameter("userId", userId)
                .setParameter("from", LocalDate.of(year, 1, 1))
                .setParameter("to", LocalDate.of(year, 12, 31))
                .getResultList();
    }
}
