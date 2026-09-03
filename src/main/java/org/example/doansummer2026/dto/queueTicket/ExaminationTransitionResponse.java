package org.example.doansummer2026.dto.queueTicket;

import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;

/** Ket qua hoan thanh mot benh an va chuyen tiep trong cung phong neu co. */
public record ExaminationTransitionResponse(
        MedicalRecordResponse completedRecord,
        QueueTicketResponse nextTicket,
        SameRoomExaminationChainResponse chain,
        boolean continuedInSameRoom
) {}
