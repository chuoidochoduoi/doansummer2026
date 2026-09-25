package vn.edu.fpt.cares.dto.testrequest;

import vn.edu.fpt.cares.enums.TestRequestStatus;

public record TestRequestUpdateRequest(
        TestRequestStatus status
) {}




