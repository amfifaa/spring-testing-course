package com.example.crashcourse.user;

import com.example.crashcourse.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
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
