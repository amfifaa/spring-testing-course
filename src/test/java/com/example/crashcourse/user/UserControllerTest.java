package com.example.crashcourse.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// "Slice" test: starts ONLY the web layer: this controller, JSON (Jackson), validation and
// @RestControllerAdvice. No database, no services. The service is a Mockito mock in the Spring context.
// What we test here is the HTTP contract: URLs, status codes, JSON shape, error format.
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc; // sends requests through Spring MVC without starting a real server

    @Autowired
    MockMvcTester mvc; // the same, with AssertJ-style assertions (Spring Framework 6.2+)

    @MockitoBean
    UserService userService;

    @Test
    void listsAllUsersAsAJsonArray() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                new User(1L, "ada", "ada@example.com", "Ada Lovelace"),
                new User(2L, "linus", "linus@example.com", "Linus Torvalds")));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].username").value("linus"));
    }

    @Test
    void returnsOneUserById() throws Exception {
        when(userService.getById(1L)).thenReturn(new User(1L, "ada", "ada@example.com", "Ada Lovelace"));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"));
    }

    @Test
    void returnsOneUserById_withMockMvcTester() {
        when(userService.getById(1L)).thenReturn(new User(1L, "ada", "ada@example.com", "Ada Lovelace"));

        assertThat(mvc.get().uri("/api/users/1"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.username").isEqualTo("ada");
    }

    @Test
    void unknownIdGivesA404ProblemDetail() throws Exception {
        when(userService.getById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("User 99 not found"));
    }

    @Test
    void createReturns201AndPassesTheRequestToTheService() throws Exception {
        CreateUserRequestDto request = new CreateUserRequestDto("ada", "ada@example.com", "Ada Lovelace");
        when(userService.create(request)).thenReturn(new User(1L, "ada", "ada@example.com", "Ada Lovelace"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "ada", "email": "ada@example.com", "fullName": "Ada Lovelace"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void invalidInputGives400AndNeverReachesTheService() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("must not be blank"))
                .andExpect(jsonPath("$.errors.email").exists());

        verify(userService, never()).create(any());
    }

    @Test
    void duplicateEmailGives409() throws Exception {
        when(userService.create(any())).thenThrow(new EmailAlreadyTakenException("ada@example.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "ada", "email": "ada@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email ada@example.com is already registered"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }
}
