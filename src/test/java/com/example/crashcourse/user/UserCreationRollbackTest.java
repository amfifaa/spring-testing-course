package com.example.crashcourse.user;

import com.example.crashcourse.IntegrationTest;
import com.example.crashcourse.notification.FakeNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// UserService.create() is @Transactional: INSERT the user, then send the welcome mail.
// If the mail fails, the INSERT must be rolled back. A unit test with a mocked repository can't prove
// that: there is no transaction and no database. Only the real proxy + the real database can.
@IntegrationTest
class UserCreationRollbackTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    // The shared fake from @IntegrationTest. In test-05 this was a @MockitoBean, which gave this
    // class its own Spring context (and its own Postgres container). Same test, no extra context.
    @Autowired
    FakeNotificationService notifications;

    @AfterEach
    void resetTheFake() {
        notifications.reset(); // the fake is shared with every other integration test
    }

    @Test
    void ifTheWelcomeMailFailsTheUserIsNotSaved() {
        notifications.failWith(new IllegalStateException("mail server down"));

        assertThatThrownBy(() -> userService.create(new CreateUserRequestDto("ada", "rollback@example.com", null)))
                .hasMessage("mail server down");

        // The INSERT did run, but the transaction was rolled back: the row is gone.
        assertThat(userRepository.findByEmail("rollback@example.com")).isEmpty();
    }
}
