package com.miniclub.repository;

import com.miniclub.model.EventRegistration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    long countByEventId(Long eventId);

    /** Rows of [eventId, registrationCount]. */
    @Query("select r.event.id, count(r) from EventRegistration r where r.event.id in :ids group by r.event.id")
    List<Object[]> countFor(@Param("ids") Collection<Long> ids);

    /** Rows of [userId, registrationCount]. */
    @Query("select r.user.id, count(r) from EventRegistration r group by r.user.id")
    List<Object[]> countPerUser();

    @Query("select r.event.id from EventRegistration r where r.user.id = :userId")
    List<Long> eventIdsOf(@Param("userId") Long userId);

    @Query("select r from EventRegistration r join fetch r.event e join fetch e.club "
            + "where r.user.id = :userId order by e.startsAt asc")
    List<EventRegistration> findByUserWithEvent(@Param("userId") Long userId);

    @Query("select r from EventRegistration r join fetch r.user where r.event.id = :eventId order by r.registeredAt asc")
    List<EventRegistration> findAttendees(@Param("eventId") Long eventId);
}
