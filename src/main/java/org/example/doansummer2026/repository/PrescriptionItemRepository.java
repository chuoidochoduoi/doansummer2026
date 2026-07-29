package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {
    List<PrescriptionItem> findByMedicalRecord_RecordId(UUID recordId);
    void deleteByMedicalRecord_RecordId(UUID recordId);
}



