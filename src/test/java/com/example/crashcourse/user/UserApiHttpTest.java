package com.example.crashcourse.user;

import com.example.crashcourse.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

// The same app, but now over REAL HTTP: Tomcat starts on a random free port and
// RestTestClient sends genuine HTTP requests to it, exactly like an external client would.
// Use this when the network layer matters (filters, real serialization, headers, the server itself).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class UserApiHttpTest {

    @Autowired
    RestTestClient client; // already pointed at http://localhost:<random port>

    @Test
    void createdUserCanBeFetchedOverHttp() {
        User created = client.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateUserRequestDto("ken", "ken@example.com", "Ken Thompson"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .returnResult().getResponseBody();

        assertThat(created).isNotNull();

        client.get().uri("/api/users/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .isEqualTo(created);
    }

    @Test
    void unknownUserGives404OverHttp() {
        client.get().uri("/api/users/{id}", 123456)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
