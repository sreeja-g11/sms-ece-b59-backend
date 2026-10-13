package com.miniclub.model;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "clubs")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ClubCategory category;

    @Column(nullable = false, length = 160)
    private String tagline;

    @Column(nullable = false, length = 2000)
    private String description;

    /** Student who leads the club (free text, e.g. "Sai Kiran, III CSE"). */
    @Column(name = "club_head", length = 100)
    private String clubHead;

    @Column(name = "faculty_advisor", length = 100)
    private String facultyAdvisor;

    @Column(name = "meeting_info", length = 120)
    private String meetingInfo;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    /** false = archived by the admin: hidden from students, no new joins or registrations. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Club() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ClubCategory getCategory() { return category; }
    public void setCategory(ClubCategory category) { this.category = category; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getClubHead() { return clubHead; }
    public void setClubHead(String clubHead) { this.clubHead = clubHead; }
    public String getFacultyAdvisor() { return facultyAdvisor; }
    public void setFacultyAdvisor(String facultyAdvisor) { this.facultyAdvisor = facultyAdvisor; }
    public String getMeetingInfo() { return meetingInfo; }
    public void setMeetingInfo(String meetingInfo) { this.meetingInfo = meetingInfo; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
