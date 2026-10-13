package com.miniclub.repository;

import com.miniclub.model.Announcement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** The whole notice board: pinned first, then newest first. Filtering by audience happens in the service. */
    @Query("select a from Announcement a left join fetch a.club join fetch a.postedBy "
            + "order by a.pinned desc, a.createdAt desc")
    List<Announcement> findBoard();

    @Query("select a from Announcement a left join fetch a.club join fetch a.postedBy where a.id = :id")
    Optional<Announcement> findWithDetails(@Param("id") Long id);
}
