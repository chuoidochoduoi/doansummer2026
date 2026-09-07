package org.example.doansummer2026;

import org.example.doansummer2026.service.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** External boundaries are never invoked by Spring/H2 tests. */
abstract class IsolatedIntegrationTest {
    @MockitoBean EmailService emailService;
    @MockitoBean SmsService smsService;
    @MockitoBean PayOSService payOSService;
    @MockitoBean BhxhIntegrationService bhxhIntegrationService;
    @MockitoBean StringRedisTemplate redisTemplate;
    @MockitoBean SystemCleanupService systemCleanupService;
}
