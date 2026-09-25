package vn.edu.fpt.cares.repository;

import vn.edu.fpt.cares.model.FamilyMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {

    @EntityGraph(attributePaths = {"ownerProfile", "memberProfile"})
    List<FamilyMember> findAllByOwnerProfile_ProfileIdOrderByCreatedAtAsc(UUID ownerProfileId);

    @EntityGraph(attributePaths = {"ownerProfile", "memberProfile"})
    List<FamilyMember> findAllByOwnerProfile_ProfileIdAndIsActiveTrueOrderByCreatedAtAsc(UUID ownerProfileId);

    @EntityGraph(attributePaths = {"ownerProfile", "memberProfile"})
    Optional<FamilyMember> findByFamilyMemberIdAndOwnerProfile_ProfileId(UUID familyMemberId, UUID ownerProfileId);

    @EntityGraph(attributePaths = {"ownerProfile", "memberProfile"})
    Optional<FamilyMember> findByOwnerProfile_ProfileIdAndMemberProfile_ProfileId(UUID ownerProfileId, UUID memberProfileId);

    @EntityGraph(attributePaths = {"ownerProfile", "memberProfile"})
    Optional<FamilyMember> findByMemberProfile_ProfileId(UUID memberProfileId);
}
