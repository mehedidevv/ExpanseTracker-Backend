package com.example.expensetracker.category;

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

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    /** NULL = system default category (read-only for users). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type;

    @Column(length = 30)
    private String icon;

    @Column(length = 30)
    private String color;

    /** Required by JPA. */
    protected Category() {
    }

    public Category(User user, String name, CategoryType type, String icon, String color) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.color = color;
    }

    public boolean isSystemDefault() {
        return user == null;
    }

    // ---- getters / setters ----

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
