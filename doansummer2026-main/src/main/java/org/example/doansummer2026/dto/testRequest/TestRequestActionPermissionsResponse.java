package org.example.doansummer2026.dto.testRequest;

public record TestRequestActionPermissionsResponse(
        boolean canView,
        boolean canEditResult,
        boolean canUpload,
        boolean canSign,
        boolean canCancel
) {
    public static TestRequestActionPermissionsResponse viewOnly() {
        return new TestRequestActionPermissionsResponse(true, false, false, false, false);
    }
}
