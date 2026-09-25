package vn.edu.fpt.cares.repository;

import jakarta.persistence.LockModeType;
import vn.edu.fpt.cares.model.MembershipCard;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface MembershipCardRepository extends JpaRepository<MembershipCard, UUID> {
    Optional<MembershipCard> findByOwnerProfile_ProfileId(UUID profileId);
    Optional<MembershipCard> findByCardCode(String cardCode);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from MembershipCard c join fetch c.ownerProfile where c.cardId = :id")
    Optional<MembershipCard> findByIdForUpdate(@Param("id") UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from MembershipCard c join fetch c.ownerProfile where c.cardCode = :code")
    Optional<MembershipCard> findByCodeForUpdate(@Param("code") String code);
}
