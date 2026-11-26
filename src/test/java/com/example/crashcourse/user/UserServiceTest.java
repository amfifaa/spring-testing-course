package com.example.crashcourse.user;

import com.example.crashcourse.notification.WelcomeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Plain unit test: NO Spring. Runs in milliseconds.
// UserService gets its dependencies through the constructor, so we can hand it mocks.
// MockitoExtension creates the @Mock fields and passes them into the @InjectMocks constructor.
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    WelcomeService welcomeService;

    @InjectMocks
    UserService userService;

    @Test
    void createSavesTheUserAndSendsAWelcomeMail() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(new User(1L, "ada", "ada@example.com", "Ada Lovelace"));

        User created = userService.create(new CreateUserRequestDto("ada", "ada@example.com", "Ada Lovelace"));

        assertThat(created.id()).isEqualTo(1L);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().id()).isNull();
        assertThat(saved.getValue().username()).isEqualTo("ada");

        verify(welcomeService).welcome("ada@example.com");
    }

    @Test
    void createRejectsADuplicateEmailAndSavesNothing() {
        when(userRepository.findByEmail("ada@example.com"))
                .thenReturn(Optional.of(new User(1L, "ada", "ada@example.com", null)));

        assertThatThrownBy(() -> userService.create(new CreateUserRequestDto("ada2", "ada@example.com", null)))
                .isInstanceOf(EmailAlreadyTakenException.class)
                .hasMessage("Email ada@example.com is already registered");

        verify(userRepository, never()).save(any());
        verify(welcomeService, never()).welcome(any());
    }

    @Test
    void getByIdThrowsWhenTheUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User 99 not found");
    }

    @Test
    void deleteOfAnUnknownUserThrowsAndDeletesNothing() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L)).isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).deleteById(any());
    }
}
