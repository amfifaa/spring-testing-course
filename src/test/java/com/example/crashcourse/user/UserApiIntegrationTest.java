package com.example.crashcourse.user;

import com.example.crashcourse.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Integration test: the WHOLE application (every bean, the real service, the real repository)
// + a real Postgres in Docker (Testcontainers) + the Liquibase migrations.
// MockMvc still skips the network: requests go straight into Spring MVC, but everything behind it is real.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    // The database is SHARED by every test that uses this context, and nothing is rolled back here.
    // Without this, a test that counts users would see whatever earlier tests left behind,
    // and pass or fail depending on the order the tests happen to run in.
    @BeforeEach
    void startWithAnEmptyTable() {
        userRepository.deleteAll();
    }

    @Test
    void listShowsExactlyTheUsersInTheDatabase() throws Exception {
        userRepository.save(new User(null, "ada", "ada@example.com", null));
        userRepository.save(new User(null, "linus", "linus@example.com", null));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void aTooLongFullNameIsRejectedWith400() throws Exception {
        // The column is varchar(255). Before @Size(max = 255) was added to CreateUserRequestDto,
        // validation let this through and Postgres rejected the INSERT: the client got a 500.
        // Only a test against the REAL database could find that: the limit isn't in any Java code.
        String tooLong = "x".repeat(256);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "long", "email": "long@example.com", "fullName": "%s"}
                                """.formatted(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    void aUserCanBeCreatedReadAndDeleted() throws Exception {
        // create
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "grace", "email": "grace@example.com", "fullName": "Grace Hopper"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(response, "$.id");

        // read it back: it really is in the database now
        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Grace Hopper"));

        // delete it
        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isNoContent());

        // and it's gone
        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void theSecondUserWithTheSameEmailGets409() throws Exception {
        // No mocks: the service asks the real repository, which asks the real database.
        String body = """
                {"username": "linus", "email": "linus@example.com"}
                """;

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}
