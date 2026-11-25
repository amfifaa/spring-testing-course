# Testing Spring Boot

A 90-minute, demo-driven session on testing Spring Boot applications: from plain unit tests to
full integration tests against a real Postgres, and how to keep the test suite fast.

The app under test is a small REST API for users (Spring Boot 4, Spring Data JDBC, Liquibase, Postgres).

**Presenter script and timings: [TESTING.md](TESTING.md)**

## Requirements

- JDK 25
- Docker (tests start their own Postgres with Testcontainers)
- An IDE; IntelliJ IDEA is recommended (`requests.http` runs in its HTTP client)

## Run

```bash
./mvnw verify            # runs all tests (Testcontainers starts its own Postgres)
./mvnw spring-boot:run   # starts Postgres via compose.yaml, runs Liquibase, serves http://localhost:8080
```

## Steps

Each step is a git branch that adds tests on top of the previous one. Every branch builds and its tests pass.

| Branch | Topic |
|-----|-------|
| `test-00` | The app, with a single "does the context start?" test |
| `test-01` | Unit tests: JUnit 5, AssertJ, Mockito, parameterized tests |
| `test-02` | Web slice: `@WebMvcTest` and MockMvc |
| `test-03` | Data slice: `@DataJdbcTest` against real Postgres |
| `test-04` | Integration I: the whole app with `@SpringBootTest` |
| `test-05` | Integration II: what only integration tests catch |
| `test-06` | Test speed: context caching and one shared setup |

```bash
git checkout -f test-02                 # jump to a step
git diff test-01 test-02                # see what a step added
```
