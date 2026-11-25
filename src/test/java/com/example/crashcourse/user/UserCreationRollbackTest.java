package com.example.crashcourse.user;

import com.example.crashcourse.TestcontainersConfiguration;
import com.example.crashcourse.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

// UserService.create() is @Transactional: INSERT the user, then send the welcome mail.
// If the mail fails, the INSERT must be rolled back. A unit test with a mocked repository can't prove
// that: there is no transaction and no database. Only the real proxy + the real database can.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserCreationRollbackTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    // Replaces the real NotificationService bean in THIS test's context with a mock.
    // Note: a different set of beans means a different Spring context (see test-06).
    @MockitoBean
    NotificationService notificationService;

    @Test
    void ifTheWelcomeMailFailsTheUserIsNotSaved() {
        doThrow(new IllegalStateException("mail server down")).when(notificationService).send(any(), any());

        assertThatThrownBy(() -> userService.create(new CreateUserRequestDto("ada", "rollback@example.com", null)))
                .hasMessage("mail server down");

        // The INSERT did run, but the transaction was rolled back: the row is gone.
        assertThat(userRepository.findByEmail("rollback@example.com")).isEmpty();
    }
}
