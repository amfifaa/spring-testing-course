package com.example.crashcourse.notification;

import com.example.crashcourse.AppProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// The greeting depends on the time of day. Because WelcomeService gets a Clock injected
// (instead of calling LocalTime.now() directly), the test can freeze time at any hour it likes.
class WelcomeServiceTest {

    // A hand-written FAKE instead of a Mockito mock: it just records what was sent.
    // For a one-method interface this is often simpler and reads better than when(...)/verify(...).
    static class RecordingNotificationService implements NotificationService {
        final List<String> sent = new ArrayList<>();

        @Override
        public void send(String to, String message) {
            sent.add(to + ": " + message);
        }
    }

    private final AppProperties properties = new AppProperties("Welcome aboard!", "no-reply@example.com");

    // One test method, run once per row. The interesting rows are the BOUNDARIES: 11:59 and 12:00.
    @ParameterizedTest(name = "at {0} the greeting is \"{1}\"")
    @CsvSource({
            "06:00, Good morning",
            "11:59, Good morning",
            "12:00, Hello",
            "23:30, Hello"
    })
    void greetingDependsOnTheTimeOfDay(LocalTime time, String expectedGreeting) {
        RecordingNotificationService notifications = new RecordingNotificationService();
        WelcomeService welcomeService = new WelcomeService(notifications, clockAt(time), properties);

        welcomeService.welcome("ada@example.com");

        assertThat(notifications.sent)
                .containsExactly("ada@example.com: " + expectedGreeting + ". Welcome aboard!");
    }

    private static Clock clockAt(LocalTime time) {
        return Clock.fixed(LocalDate.of(2026, 1, 15).atTime(time).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }
}
