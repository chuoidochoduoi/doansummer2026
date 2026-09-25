package vn.edu.fpt.cares.dto.queueticket;

import vn.edu.fpt.cares.dto.medicalrecord.MedicalRecordResponse;

/** Ket qua hoan thanh mot benh an va chuyen tiep trong cung phong neu co. */
public record ExaminationTransitionResponse(
        MedicalRecordResponse completedRecord,
        QueueTicketResponse nextTicket,
        SameRoomExaminationChainResponse chain,
        boolean continuedInSameRoom
) {}
