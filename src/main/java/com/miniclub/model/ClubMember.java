package com.miniclub.model;

import jakarta.persistence.*;
import java.time.Instant;

/** A student who has joined a club. Leaving the club deletes the row. */
@Entity
@Table(name = "club_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_club_user", columnNames = {"club_id", "user_id"}))
public class ClubMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    void onCreate() {
        joinedAt = Instant.now();
    }

    public ClubMember() {
    }

    public ClubMember(Club club, User user) {
        this.club = club;
        this.user = user;
    }

    public Long getId() { return id; }
    public Club getClub() { return club; }
    public User getUser() { return user; }
    public Instant getJoinedAt() { return joinedAt; }
}
