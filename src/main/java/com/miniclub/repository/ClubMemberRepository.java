package com.miniclub.repository;

import com.miniclub.model.ClubMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    boolean existsByClubIdAndUserId(Long clubId, Long userId);

    Optional<ClubMember> findByClubIdAndUserId(Long clubId, Long userId);

    long countByClubId(Long clubId);

    /** Rows of [clubId, memberCount]. */
    @Query("select m.club.id, count(m) from ClubMember m group by m.club.id")
    List<Object[]> countPerClub();

    /** Rows of [userId, clubCount]. */
    @Query("select m.user.id, count(m) from ClubMember m group by m.user.id")
    List<Object[]> countPerUser();

    @Query("select m.club.id from ClubMember m where m.user.id = :userId")
    List<Long> clubIdsOf(@Param("userId") Long userId);

    @Query("select m from ClubMember m join fetch m.user where m.club.id = :clubId order by m.joinedAt asc")
    List<ClubMember> findMembers(@Param("clubId") Long clubId);

    @Query("select count(m) from ClubMember m where m.club.active = true")
    long countInActiveClubs();
}
