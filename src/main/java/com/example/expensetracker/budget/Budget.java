package com.example.expensetracker.budget;

import com.example.expensetracker.category.Category;
import com.example.expensetracker.common.entity.BaseEntity;
import com.example.expensetracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "budgets")
public class Budget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** NULL = overall monthly budget. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "year", nullable = false)
    private Integer year;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "month", nullable = false)
    private Integer month;

    /** Required by JPA. */
    protected Budget() {
    }

    public Budget(User user, Category category, BigDecimal amount, Integer year, Integer month) {
        this.user = user;
        this.category = category;
        this.amount = amount;
        this.year = year;
        this.month = month;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }
}
