package com.example.crashcourse.notification;

import java.util.ArrayList;
import java.util.List;

// A test double the integration tests can control: it records what was sent,
// and can be switched to fail like a mail server that is down.
public class FakeNotificationService implements NotificationService {

    private final List<String> sent = new ArrayList<>();
    private RuntimeException failure;

    @Override
    public void send(String to, String message) {
        if (failure != null) {
            throw failure;
        }
        sent.add(to + ": " + message);
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public List<String> sent() {
        return sent;
    }

    // The fake lives as long as the shared context, so tests must reset it after use.
    public void reset() {
        sent.clear();
        failure = null;
    }
}
