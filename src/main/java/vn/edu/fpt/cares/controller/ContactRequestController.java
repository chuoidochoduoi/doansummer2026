package vn.edu.fpt.cares.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.dto.contact.ContactRequestCreateRequest;
import vn.edu.fpt.cares.service.ContactRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ContactRequestController {

    private final ContactRequestService service;

    @PostMapping("/api/public/contact-requests")
    public ResponseEntity<Map<String, String>> send(
            @Valid @RequestBody ContactRequestCreateRequest request) {
        service.send(request);
        return ResponseEntity.ok(Map.of("message", "Thông tin liên hệ đã được gửi tới CareS"));
    }
}
