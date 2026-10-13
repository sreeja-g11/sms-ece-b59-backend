package com.miniclub.repository;

import com.miniclub.model.Event;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** Events that haven't finished yet, soonest first. clubId = null means every club. */
    @Query("select e from Event e join fetch e.club c "
            + "where e.endsAt > :now and (:clubId is null or c.id = :clubId) order by e.startsAt asc")
    List<Event> findUpcoming(@Param("now") Instant now, @Param("clubId") Long clubId);

    /** Finished events, most recent first. */
    @Query("select e from Event e join fetch e.club c "
            + "where e.endsAt <= :now and (:clubId is null or c.id = :clubId) order by e.startsAt desc")
    List<Event> findPast(@Param("now") Instant now, @Param("clubId") Long clubId);

    @Query("select e from Event e join fetch e.club where e.id = :id")
    Optional<Event> findWithClub(@Param("id") Long id);

    /** SELECT ... FOR UPDATE: stops two students from taking the last seat at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> lockById(@Param("id") Long id);

    @Query("select count(e) from Event e where e.cancelled = false and e.endsAt > :now and e.club.active = true")
    long countUpcoming(@Param("now") Instant now);

    /** Rows of [clubId, upcomingEventCount] for events that are not cancelled and not finished. */
    @Query("select e.club.id, count(e) from Event e where e.cancelled = false and e.endsAt > :now group by e.club.id")
    List<Object[]> countUpcomingPerClub(@Param("now") Instant now);
}
