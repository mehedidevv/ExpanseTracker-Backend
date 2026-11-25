package com.example.expensetracker.user;

import com.example.expensetracker.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    /** Always stored lowercase. */
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash. */
    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    /** False until the registration OTP is verified. Unverified users cannot log in. */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /** Required by JPA. */
    protected User() {
    }

    public User(String email, String password, String fullName, String currencyCode) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.currencyCode = currencyCode;
    }

    // ---- getters / setters ----

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
