package com.example.expensetracker.transaction;

import com.example.expensetracker.transaction.dto.TransactionFilter;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Dynamic filters for GET /transactions (FR-T3). */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> forUser(UUID userId, TransactionFilter f) {
        Specification<Transaction> spec = (root, q, cb) -> cb.equal(root.get("user").get("id"), userId);
        if (f == null) {
            return spec;
        }
        if (f.type() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("type"), f.type()));
        }
        if (f.categoryId() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category").get("id"), f.categoryId()));
        }
        if (f.from() != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), f.from()));
        }
        if (f.to() != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), f.to()));
        }
        if (f.minAmount() != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), f.minAmount()));
        }
        if (f.maxAmount() != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("amount"), f.maxAmount()));
        }
        if (f.q() != null && !f.q().isBlank()) {
            String pattern = "%" + f.q().trim().toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("note")), pattern, '\\'));
        }
        return spec;
    }
}
