package com.miniclub.model;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 100)
    private String department;

    /** Year of study, 1-4 (students only). */
    @Column(name = "study_year")
    private Integer year;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public User() {
    }

    public User(String name, String email, String passwordHash, Role role, String department, Integer year) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.department = department;
        this.year = year;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** Kept for the JWT filter: every account in this portal is active. */
    public boolean isActive() {
        return true;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getDepartment() { return department; }
    public Integer getYear() { return year; }
    public Instant getCreatedAt() { return createdAt; }
}
