package com.miniclub.dto;

import com.miniclub.model.Announcement;
import com.miniclub.model.ClubCategory;
import java.time.Instant;

/** clubId / clubName are null for college-wide announcements. */
public record AnnouncementView(Long id, String title, String body, boolean pinned, Long clubId, String clubName,
                               ClubCategory clubCategory, String postedBy, Instant createdAt, Instant updatedAt) {

    public static AnnouncementView from(Announcement a) {
        var c = a.getClub();
        return new AnnouncementView(a.getId(), a.getTitle(), a.getBody(), a.isPinned(),
                c == null ? null : c.getId(), c == null ? null : c.getName(), c == null ? null : c.getCategory(),
                a.getPostedBy().getName(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
