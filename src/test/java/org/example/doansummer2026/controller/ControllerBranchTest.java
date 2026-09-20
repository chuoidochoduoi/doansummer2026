package org.example.doansummer2026.controller;

import org.example.doansummer2026.dto.auth.SendOtpRequest;
import org.example.doansummer2026.dto.journey.PatientJourneyResponse;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.service.*;
import org.example.doansummer2026.service.interfaces.AuthServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.payos.model.webhooks.WebhookData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ControllerBranchTest {

    @Test
    void authOtpEndpointsExposeCodeOnlyWhenConfigured() {
        AuthServiceInterface auth = mock(AuthServiceInterface.class);
        OtpService otp = mock(OtpService.class);
        AuthController controller = new AuthController(auth, otp);
        SendOtpRequest request = new SendOtpRequest("0909000001");
        when(otp.sendOtp(request.identifier())).thenReturn("123456");

        assertNull(controller.sendOtp(request).getBody().mockOtp());
        ReflectionTestUtils.setField(controller, "exposeOtpCode", true);
        assertEquals("123456", controller.sendOtp(request).getBody().mockOtp());
        assertEquals("123456", controller.sendRegisterOtp(request).getBody().mockOtp());
        verify(auth).ensureRegistrationIdentifierAvailable(request.identifier());
    }

    @Test
    void notificationRecipientGuardHidesOtherProfilesAndAcceptsOwner() {
        NotificationService notifications = mock(NotificationService.class);
        AuthService auth = mock(AuthService.class);
        ProfileService profiles = mock(ProfileService.class);
        NotificationController controller = new NotificationController(notifications, auth, profiles);
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        when(auth.currentProfileId()).thenReturn(null, other, owner);
        assertThrows(ResourceNotFoundException.class, () -> controller.unreadCount(owner));
        assertThrows(ResourceNotFoundException.class, () -> controller.unreadCount(owner));
        controller.unreadCount(owner);
        verify(notifications).unreadCount(owner);
    }

    @Test
    void patientJourneyMineSupportsExplicitFamilyAndOwnerScopes() {
        PatientJourneyService journeys = mock(PatientJourneyService.class);
        AuthService auth = mock(AuthService.class);
        FamilyAccessService family = mock(FamilyAccessService.class);
        PatientJourneyController controller = new PatientJourneyController(journeys, auth, family);
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        Account account = Account.builder().accountId(accountId).build();
        Profile owner = Profile.builder().profileId(ownerId).build();
        Profile child = Profile.builder().profileId(childId).build();
        PatientJourneyResponse ownerJourney = mock(PatientJourneyResponse.class);
        PatientJourneyResponse childJourney = mock(PatientJourneyResponse.class);
        when(auth.currentAccount()).thenReturn(account);
        when(family.resolveReadableProfile(accountId, childId)).thenReturn(child);
        when(family.readableProfiles(accountId, true)).thenReturn(List.of(owner, child));
        when(family.ownerProfile(accountId)).thenReturn(owner);
        when(journeys.listForCustomer(ownerId)).thenReturn(List.of(ownerJourney));
        when(journeys.listForCustomer(childId)).thenReturn(List.of(childJourney));

        assertEquals(List.of(childJourney), controller.mine(childId, false).getBody());
        assertEquals(2, controller.mine(null, true).getBody().size());
        assertEquals(List.of(ownerJourney), controller.mine(null, false).getBody());
    }

    @Test
    void payOsWebhookCoversPaidUnknownAndInvalidPayloads() throws Exception {
        PayOSService payOS = mock(PayOSService.class);
        InvoiceService invoices = mock(InvoiceService.class);
        PayOSWebhookController controller = new PayOSWebhookController(payOS, invoices);
        WebhookData data = mock(WebhookData.class);
        UUID invoiceId = UUID.randomUUID();
        when(data.getOrderCode()).thenReturn(101L);
        when(payOS.verifyWebhook(any())).thenReturn(data);
        when(payOS.getInvoiceIdByOrderCode(101L)).thenReturn(invoiceId, (UUID) null);

        Map<?, ?> paid = (Map<?, ?>) controller.handleWebhook("{}").getBody();
        Map<?, ?> unknown = (Map<?, ?>) controller.handleWebhook("{}").getBody();
        assertEquals(true, paid.get("success"));
        assertEquals(true, unknown.get("success"));
        verify(invoices).pay(invoiceId, null);

        Map<?, ?> malformed = (Map<?, ?>) controller.handleWebhook("not-json").getBody();
        assertEquals(false, malformed.get("success"));
        assertNotNull(malformed.get("message"));
    }
}
