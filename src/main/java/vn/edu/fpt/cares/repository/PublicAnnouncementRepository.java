package vn.edu.fpt.cares.repository;

import vn.edu.fpt.cares.model.PublicAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PublicAnnouncementRepository extends JpaRepository<PublicAnnouncement, UUID> {
    List<PublicAnnouncement> findAllByOrderByCreatedAtDesc();

    @Query("""
            select value from PublicAnnouncement value
            where value.published = true
              and (value.startsAt is null or value.startsAt <= :now)
              and (value.endsAt is null or value.endsAt >= :now)
            order by value.createdAt desc
            """)
    List<PublicAnnouncement> findVisible(LocalDateTime now);
}
