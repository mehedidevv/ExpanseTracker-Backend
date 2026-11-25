package com.example.expensetracker.transaction;

import com.example.expensetracker.category.Category;
import com.example.expensetracker.category.CategoryType;
import com.example.expensetracker.common.entity.BaseEntity;
import com.example.expensetracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    /** The day the money moved (DATE, not a timestamp). */
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /** Required by JPA. */
    protected Transaction() {
    }

    public Transaction(User user, Category category, CategoryType type, BigDecimal amount, String note,
                       LocalDate transactionDate) {
        this.user = user;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.transactionDate = transactionDate;
    }

    // ---- getters / setters ----

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
}
