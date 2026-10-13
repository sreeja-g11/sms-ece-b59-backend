package com.miniclub.model;

import jakarta.persistence.*;
import java.time.Instant;

/** A student's seat at an event. Cancelling the registration deletes the row. */
@Entity
@Table(name = "event_registrations",
        uniqueConstraints = @UniqueConstraint(name = "uk_registration_event_user", columnNames = {"event_id", "user_id"}))
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    @PrePersist
    void onCreate() {
        registeredAt = Instant.now();
    }

    public EventRegistration() {
    }

    public EventRegistration(Event event, User user) {
        this.event = event;
        this.user = user;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public User getUser() { return user; }
    public Instant getRegisteredAt() { return registeredAt; }
}
