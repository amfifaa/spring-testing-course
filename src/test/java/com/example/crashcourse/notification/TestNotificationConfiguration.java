package com.example.crashcourse.notification;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// Part of the shared @IntegrationTest setup: replaces the real notification channel with a fake
// that every integration test gets. Because it's ALWAYS there, tests that need a failing mail
// server don't need a @MockitoBean (which would force a separate context).
@TestConfiguration(proxyBeanMethods = false)
public class TestNotificationConfiguration {

    @Bean
    @Primary // wins over the app's own NotificationService bean
    FakeNotificationService fakeNotificationService() {
        return new FakeNotificationService();
    }
}
