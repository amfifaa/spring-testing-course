package com.example.crashcourse;

import com.example.crashcourse.notification.TestNotificationConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// ONE setup for every integration test.
// Spring caches application contexts by their configuration: test classes with exactly the same
// annotations, imports and mocks share one context (and one database), started once per test run.
// Every small difference (another webEnvironment, a @MockitoBean, an extra @Import) means another
// context to start. So all integration tests use this annotation and nothing else.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@Import({TestcontainersConfiguration.class, TestNotificationConfiguration.class})
public @interface IntegrationTest {
}
