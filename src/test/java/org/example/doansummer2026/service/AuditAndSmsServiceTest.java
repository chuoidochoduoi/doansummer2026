package org.example.doansummer2026.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import org.example.doansummer2026.config.SmsProperties;
import org.example.doansummer2026.dto.auditlog.AuditLogCreateRequest;
import org.example.doansummer2026.enums.AuditAction;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.AuditLog;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.AuditLogRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditAndSmsServiceTest {

    @Test
    void auditSearchResolvesProfileUsernameAndSystemActors() {
        AuditLogRepository logs = mock(AuditLogRepository.class);
        AccountRepository accounts = mock(AccountRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        AuditLogService service = new AuditLogService(logs, accounts, profiles);
        UUID profileActor = UUID.randomUUID();
        UUID accountActor = UUID.randomUUID();
        AuditLog first = audit(profileActor);
        AuditLog second = audit(accountActor);
        AuditLog system = audit(null);
        var pageable = PageRequest.of(0, 10);
        when(logs.search(isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(first, second, system), pageable, 3));
        when(profiles.findFirstByAccount_AccountId(profileActor))
                .thenReturn(Optional.of(Profile.builder().fullName("Nguyễn An").build()));
        when(profiles.findFirstByAccount_AccountId(accountActor)).thenReturn(Optional.empty());
        when(accounts.findById(accountActor))
                .thenReturn(Optional.of(Account.builder().username("fallback.user").build()));

        var result = service.search(null, null, null, null, null, pageable);

        assertEquals("Nguyễn An", result.content().get(0).actorName());
        assertEquals("fallback.user", result.content().get(1).actorName());
        assertNull(result.content().get(2).actorName());
    }

    @Test
    void auditCreatePersistsAllTraceFields() {
        AuditLogRepository logs = mock(AuditLogRepository.class);
        AuditLogService service = new AuditLogService(logs, mock(AccountRepository.class), mock(ProfileRepository.class));
        when(logs.save(any())).thenAnswer(call -> {
            AuditLog value = call.getArgument(0);
            value.setAuditId(UUID.randomUUID());
            return value;
        });
        var request = new AuditLogCreateRequest(AuditAction.UPDATE, "Invoice", "INV-1", UUID.randomUUID(),
                "127.0.0.1", "JUnit", "old", "new", "Cập nhật hóa đơn");

        var response = service.create(request);

        assertNotNull(response.auditId());
        assertEquals("Invoice", response.entityName());
        assertEquals("Cập nhật hóa đơn", response.description());
    }

    @Test
    void auditFindByEntityMapsEveryEntryAndMissingActorName() {
        AuditLogRepository logs = mock(AuditLogRepository.class);
        AccountRepository accounts = mock(AccountRepository.class);
        ProfileRepository profiles = mock(ProfileRepository.class);
        AuditLogService service = new AuditLogService(logs, accounts, profiles);
        UUID actorId = UUID.randomUUID();
        when(logs.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Invoice", "INV-1"))
                .thenReturn(List.of(audit(actorId)));
        when(profiles.findFirstByAccount_AccountId(actorId)).thenReturn(Optional.empty());
        when(accounts.findById(actorId)).thenReturn(Optional.empty());

        var result = service.findByEntity("Invoice", "INV-1");

        assertEquals(1, result.size());
        assertNull(result.get(0).actorName());
    }

    @Test
    void twilioSmsBuildsExpectedMessageWithoutCallingTheNetwork() {
        SmsProperties properties = new SmsProperties("twilio",
                new SmsProperties.Twilio("sid", "token", "+10000000000"), null);
        TwilioSmsService service = new TwilioSmsService(properties);
        MessageCreator creator = mock(MessageCreator.class);
        try (var message = mockStatic(Message.class)) {
            message.when(() -> Message.creator(any(com.twilio.type.PhoneNumber.class),
                            any(com.twilio.type.PhoneNumber.class), anyString()))
                    .thenReturn(creator);

            service.sendOtp("0901234567", "123456");

            message.verify(() -> Message.creator(eq(new com.twilio.type.PhoneNumber("+84901234567")),
                    eq(new com.twilio.type.PhoneNumber("+10000000000")),
                    contains("123456")));
            verify(creator).create();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void zaloSmsPostsExpectedTemplatePayloadWithoutCallingTheNetwork() {
        SmsProperties properties = new SmsProperties("zalo", null,
                new SmsProperties.Zalo("app", "secret", "otp-template"));
        ZaloSmsService service = new ZaloSmsService(properties);
        RestTemplate restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        service.sendOtp("84901234567", "654321");

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq("https://openapi.zalo.me/v3.0/oa/message"),
                requestCaptor.capture(), eq(String.class));
        HttpEntity<java.util.Map<String, Object>> request = requestCaptor.getValue();
        assertEquals(org.springframework.http.MediaType.APPLICATION_JSON,
                request.getHeaders().getContentType());
        var body = request.getBody();
        assertNotNull(body);
        assertEquals("otp-template", body.get("template_id"));
        assertEquals(java.util.Map.of("phone", "84901234567"), body.get("recipient"));
        assertEquals(java.util.Map.of("code", "654321"), body.get("template_data"));
    }

    @Test
    void mockSmsAcceptsMaskedShortNormalAndNullPhoneNumbers() {
        MockSmsService service = new MockSmsService();
        assertAll(
                () -> assertDoesNotThrow(() -> service.sendOtp(null, "123456")),
                () -> assertDoesNotThrow(() -> service.sendOtp("09", "123456")),
                () -> assertDoesNotThrow(() -> service.sendOtp("0909000123", "123456"))
        );
    }

    private AuditLog audit(UUID actorId) {
        return AuditLog.builder().auditId(UUID.randomUUID()).action(AuditAction.UPDATE)
                .entityName("Invoice").entityId("INV-1").actorAccountId(actorId).build();
    }
}
