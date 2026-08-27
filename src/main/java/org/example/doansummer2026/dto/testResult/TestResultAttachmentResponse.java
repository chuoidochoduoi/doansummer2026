package org.example.doansummer2026.dto.testResult;

import org.example.doansummer2026.model.TestResultAttachment;

import java.util.UUID;

public record TestResultAttachmentResponse(
        UUID attachmentId, UUID revisionId, String originalName, String contentType,
        Long fileSize, Integer displayOrder, String url
) {
    public static TestResultAttachmentResponse from(TestResultAttachment attachment) {
        return new TestResultAttachmentResponse(attachment.getAttachmentId(),
                attachment.getRevision().getRevisionId(), attachment.getOriginalName(),
                attachment.getContentType(), attachment.getFileSize(), attachment.getDisplayOrder(),
                "/api/v1/test-results/attachments/" + attachment.getAttachmentId() + "/file");
    }
}
