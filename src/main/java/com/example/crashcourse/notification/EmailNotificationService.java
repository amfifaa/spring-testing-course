package com.example.crashcourse.notification;

// No @Component here: AppConfig creates this object in a @Bean method, because it picks email OR sms.
public class EmailNotificationService implements NotificationService {

    @Override
    public void send(String to, String message) {
        System.out.println("[EMAIL to " + to + "] " + message);
    }
}
