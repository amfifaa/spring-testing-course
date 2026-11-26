# Testing Spring Boot: presenter script

**Length:** 90 minutes.
**Result:** the users API covered by unit, slice and integration tests against a real Postgres, and a test suite that starts one database instead of five.

Every step is a **git branch** (`test-00` … `test-06`). Each one builds, and all its tests pass, so if a live demo goes wrong you can recover with one command:

```bash
git checkout -f test-03          # jump to a step (throws away local edits!)
git diff test-02 test-03         # show exactly what a step added (great on the projector)
```

> This file is included in every branch, so it stays open in your IDE while you switch steps.
> Don't commit on the step branches during the demo; `checkout -f` simply discards your live edits.

---

## Agenda

| Time | Min | Branch | Topic |
|------|-----|-----|-------|
| 0:00 | 10 | none | Test philosophy |
| 0:10 | 13 | `test-01` | Unit tests: JUnit 5, AssertJ, Mockito, parameterized tests |
| 0:23 | 12 | `test-02` | Web slice: `@WebMvcTest`, MockMvc |
| 0:35 | 10 | `test-03` | Data slice: `@DataJdbcTest` against real Postgres |
| 0:45 | 12 | `test-04` | Integration I: the whole app with `@SpringBootTest` |
| 0:57 | 12 | `test-05` | Integration II: what only integration tests catch |
| 1:09 | 10 | `test-06` | Test speed: context caching, one shared setup |
| 1:19 | 8  | none | Wrap-up, Q&A |

If you run late, cut `test-06` down to the before/after numbers and the "what creates a new context" list.

---

## Preparation (the day before)

**You:**
- [ ] JDK 25, IntelliJ IDEA, Docker running (`docker info` works without `sudo`).
- [ ] `docker pull postgres:17`. Venue Wi-Fi is never your friend.
- [ ] `git checkout test-06 && ./mvnw verify` downloads every dependency and runs all tests once.
- [ ] `git checkout -f test-00`
- [ ] Font size in the IDE: big. Then bigger.

**Participants** (send this a few days before):
- [ ] JDK 25 + IntelliJ IDEA + Docker Desktop (or Docker Engine) installed.
- [ ] Clone this repo and run `./mvnw verify` once on `test-06`. If it passes, they're ready.

---

## 0:00 Test philosophy (10 min, no code)

**Why we test.** Not to find bugs today, but to be able to *change* the code tomorrow without fear. A good test suite is a safety net, documentation that can't go out of date, and design feedback: code that is hard to test is usually hard to use.

**The pyramid** (and its critics):

| Kind | How many | Speed | Confidence per test | In this course |
|---|---|---|---|---|
| Unit tests | Many | Milliseconds | Low: one class, everything else faked | `test-01` |
| Integration tests | Some | ~1 s to seconds | Medium to high: real Spring, a real database | Narrow (slices): `test-02`, `test-03`. Broad (whole app): `test-04`, `test-05` |
| End-to-end tests | Fewest | Minutes | Highest: the deployed system, used the way a real client uses it | Not covered; `UserApiHttpTest` (real HTTP) is the closest |

- The "testing trophy" argues for more integration tests, because that's where real bugs hide (we'll find one in `test-05`). Both agree: fast tests for logic, real tests for wiring.
- Spring's **slices** are narrow integration tests: real Spring, but only one layer. `@SpringBootTest` is the broad kind: every layer at once.

**What makes a good test:**
- **Test behaviour, not implementation.** Assert what the code *does* (the API returns 404, the row is in the table), not *how* (which private method was called). Over-mocked tests break on every refactoring and catch nothing.
- **One reason to fail.** When it goes red, the name tells you what broke.
- **Names that read as sentences:** `createRejectsADuplicateEmailAndSavesNothing`, not `testCreate2`.
- **Given / when / then:** set up, act once, check.
- **Mock at your boundaries, not everything.** Mock what's slow or outside your control (a mail server). Don't mock the database to test your SQL: use a real one.
- **F.I.R.S.T.:** Fast, Isolated (no shared state), Repeatable (same result every run, any order), Self-validating (passes or fails, no reading logs), Timely (written with the code).
- **A test you've never seen fail proves nothing.** We'll break the code on purpose more than once today.

---

## 0:10 `test-01`: unit tests (13 min)

```bash
git checkout -f test-01
git diff test-00 test-01 --stat
```

Files: `user/UserServiceTest.java`, `notification/WelcomeServiceTest.java`.

Demo:
1. **`UserServiceTest`**: no Spring at all.
   - `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`: Mockito creates the mocks and calls the constructor. That works only because `UserService` uses **constructor injection**.
   - Read `createSavesTheUserAndSendsAWelcomeMail` top to bottom: *given* (`when(...).thenReturn(...)`), *when* (one call), *then* (AssertJ `assertThat`, `verify`).
   - **`ArgumentCaptor`**: catch the `User` that was passed to `save()` and check it (id is `null`, so it's a new row).
   - `verify(..., never())`: on a duplicate email nothing is saved and no mail goes out.
2. **`WelcomeServiceTest`**: the greeting depends on the time of day.
   - `WelcomeService` gets a `Clock` injected instead of calling `LocalTime.now()`. So the test can **freeze time**: `Clock.fixed(...)`. Design for testability in one line.
   - `@ParameterizedTest` + `@CsvSource`: one test, four rows. The rows that matter are the **boundaries**, 11:59 and 12:00.
   - A hand-written **fake** (`RecordingNotificationService`) instead of a Mockito mock. For a one-method interface it's often simpler to read.
3. **Run them.** Point at the times: the 8 unit tests take well under a second. `CrashCourseApplicationTests` (the whole app) takes 10-15 s. That ratio is the pyramid.
4. **Break it:** in `WelcomeService`, change `< 12` to `<= 12`. Only the 12:00 row fails, and its name says why. Put it back.

---

## 0:23 `test-02`: web slice with `@WebMvcTest` (12 min)

```bash
git checkout -f test-02
git diff test-01 test-02 --stat
```

Demo:
1. **`@WebMvcTest(UserController.class)`** starts only the web layer: this controller, Jackson, Bean Validation and `@RestControllerAdvice`. No service, no database. It tests the **HTTP contract**: URLs, status codes, JSON, error format.
2. **`@MockitoBean UserService`**: a Mockito mock that lives *in the Spring context*, in place of the real service.
3. **`MockMvc`** sends requests through Spring MVC without a real server. `jsonPath("$.username")` checks the JSON.
4. Walk through the contract, one test each: list (200 + array), by id (200), unknown id (**404** as `application/problem+json`), create (**201**), invalid input (**400** with each bad field), duplicate email (**409**), delete (**204**).
5. `invalidInputGives400AndNeverReachesTheService`: `verify(userService, never()).create(any())` proves validation stops the request *before* the service.
6. **`MockMvcTester`** (Spring Framework 6.2+): the same request with AssertJ-style assertions. Show `returnsOneUserById_withMockMvcTester` next to the classic version; teams can pick either.
7. **Break it:** remove `@Valid` from `UserController.create`. The 400 test fails: the service would now receive junk. Put it back.

---

## 0:35 `test-03`: data slice with `@DataJdbcTest` (10 min)

```bash
git checkout -f test-03
git diff test-02 test-03
```

Demo:
1. **New test starter** in `pom.xml`: `spring-boot-starter-data-jdbc-test`. Spring Boot 4 splits test support per technology, so each slice has its own starter.
2. **`@DataJdbcTest`** starts only Spring Data JDBC, the `DataSource` and **Liquibase**. `@Import(TestcontainersConfiguration.class)` gives it a **real Postgres** in Docker.
3. **Every test runs in a transaction that is rolled back.** `eachTestStartsWithAnEmptyTable` passes in any order.
4. `findByEmail...`: the derived query is checked against real SQL, not against a mock that returns whatever you tell it.
5. `theDatabaseRejectsADuplicateEmail`: the unique constraint lives in the **Liquibase changeset**, not in Java. A mock could never tell you it exists. Spring translates the Postgres error into its own `DuplicateKeyException`, so the test doesn't depend on the database vendor.

Point out: why not H2 (an in-memory database)? Because it isn't Postgres. Different SQL, different types, different errors. Testcontainers makes the real thing almost as convenient.

---

## 0:45 `test-04`: integration I, the whole app (12 min)

```bash
git checkout -f test-04
git diff test-03 test-04 --stat
```

Demo:
1. **`UserApiIntegrationTest`**: `@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers. Every bean is real: controller, service, repository, Liquibase, Postgres. Only the network is skipped.
   - `aUserCanBeCreatedReadAndDeleted`: one user's whole life through the API.
   - `theSecondUserWithTheSameEmailGets409`: no mocks. The service asks the real repository, which asks the real database.
2. **`UserApiHttpTest`**: the same app over **real HTTP**. `webEnvironment = RANDOM_PORT` starts Tomcat on a free port, and `RestTestClient` (Spring Framework 7) sends genuine requests to it. New test dependency: `spring-boot-resttestclient`.
3. **MockMvc or real HTTP?**

   | | MockMvc | `RANDOM_PORT` + `RestTestClient` |
   |---|---|---|
   | Server | None; calls Spring MVC directly | Real Tomcat on a random port |
   | Speed | Faster | A bit slower |
   | Test transaction | Can roll back (same thread) | Can't: the server runs on its own threads |
   | Use it for | Most API tests | Filters, real serialization, anything about the server itself |

4. **Notice the problem we just created:** these tests write to the database and *don't* clean up. `linus@example.com` is still there after the 409 test. That's the start of `test-05`.

---

## 0:57 `test-05`: integration II, what only integration tests catch (12 min)

```bash
git checkout -f test-05
git diff test-04 test-05
```

Three things a unit test can't show you:

1. **Test data isolation.** The integration tests share one database and nothing rolls back.
   - `listShowsExactlyTheUsersInTheDatabase` counts users. Without cleanup, it passes or fails depending on what ran before it. That's the classic "passes alone, fails in the suite".
   - Fix used here: `@BeforeEach` → `userRepository.deleteAll()`. Alternatives: unique data per test (random emails), `@Sql` scripts, or `@Transactional` on the test class (rolls back with MockMvc, but *not* with `RANDOM_PORT`).
2. **`@Transactional` really rolls back** (`UserCreationRollbackTest`).
   - `create()` inserts the user, *then* sends the welcome mail. A `@MockitoBean NotificationService` makes the mail fail.
   - The test checks the row is **gone**. A unit test with a mocked repository can't prove that: no transaction, no database.
   - **Break it:** remove `@Transactional` from `UserService.create`. The test fails: the user was saved although the call failed. Put it back.
3. **A real bug that only the database finds** (`aTooLongFullNameIsRejectedWith400`).
   - The `full_name` column is `varchar(255)`, but the DTO had no length check. A 300-character name passed validation, then Postgres refused the INSERT. **The client got a 500.**
   - No unit test or `@WebMvcTest` could see it: the limit isn't in any Java code.
   - The fix is one annotation: `@Size(max = 255)` on `fullName` in `CreateUserRequestDto` (it's in this branch's diff).
   - **Break it:** remove that `@Size`. The test fails with Postgres's own message: *value too long for type character varying(255)*. Put it back.

---

## 1:09 `test-06`: test speed (10 min)

```bash
git checkout -f test-06
git diff test-05 test-06 --stat
```

**Before/after** (whole `./mvnw verify`, measured on the presenter's machine):

| | `test-05` | `test-06` |
|---|---|---|
| Spring contexts started | 6 | 3 |
| Postgres containers started | 5 | 1 |
| Build time | ~28 s | ~17 s |

Show it live: run `./mvnw verify` on both branches and count the `Started … in` lines and the `Creating container for image: postgres` lines in the output.

Points:
1. **Spring caches test contexts.** Test classes with *exactly* the same configuration share one context, started once per run. Each difference makes a new context:
   - a different `webEnvironment` (MockMvc vs `RANDOM_PORT`),
   - a `@MockitoBean` / `@MockitoSpyBean`,
   - a different `@Import`, properties or active profile,
   - `@DirtiesContext` (throws the context away after use: avoid).
2. **One setup for all integration tests:** the `@IntegrationTest` meta-annotation. Every integration test uses it and nothing else, so they all share one context.
3. **A shared fake instead of `@MockitoBean`:** `FakeNotificationService` is in every integration context and can be told to fail. The rollback test is the same test, without its own context. The price: shared state, so it's `reset()` after each test.
4. **One container for the whole run:** the container in `TestcontainersConfiguration` is `static`. Every context, including the `@DataJdbcTest` slice, uses the same Postgres. The price, again, is shared data: `UserRepositoryTest` now clears the table first.
5. Speed and isolation pull against each other. Share what's expensive (contexts, containers), and keep each test's *data* to itself.

---

## 1:19 Wrap-up (8 min)

Which test for what:

| You want to check… | Test |
|---|---|
| Logic, rules, edge cases | Unit test (JUnit + Mockito or a fake) |
| URLs, status codes, JSON, validation, error format | `@WebMvcTest` |
| Queries, mappings, migrations, database constraints | `@DataJdbcTest` + Testcontainers |
| Wiring, transactions, the full request → database flow | `@SpringBootTest` + Testcontainers |
| The server and the network themselves | `RANDOM_PORT` + `RestTestClient` |

Takeaways:
1. Test behaviour, not implementation; see every test fail once.
2. Many fast unit tests, a few integration tests where the real bugs hide.
3. Use the real database (Testcontainers), not a fake one.
4. Keep the suite fast: one integration setup, one container, few contexts.

Where to go next:
- `@JsonTest` (just JSON serialization), `@RestClientTest` (your HTTP clients)
- WireMock, for stubbing external HTTP APIs
- Mutation testing (PIT): does your suite notice when the code changes?
- Spring Boot testing reference: https://docs.spring.io/spring-boot/reference/testing/

---

## Cheat sheet (print / share)

| Annotation / class | Meaning |
|---|---|
| `@Test` | A test method (JUnit 5) |
| `@ParameterizedTest` + `@CsvSource` | One test, run once per row of data |
| `@BeforeEach` / `@AfterEach` | Run before / after every test in the class |
| `@ExtendWith(MockitoExtension.class)` | Mockito for plain unit tests |
| `@Mock` / `@InjectMocks` | Create a mock / build the class under test with the mocks |
| `ArgumentCaptor` | Catch an argument passed to a mock to check it |
| `@WebMvcTest(X.class)` | Web slice: controller X, JSON, validation, advice |
| `@MockitoBean` | Replace a bean in the Spring context with a mock (creates a new context!) |
| `MockMvc` / `MockMvcTester` | Fake HTTP requests through Spring MVC; classic / AssertJ style |
| `@DataJdbcTest` | Data slice: repositories + database + Liquibase, rolled back per test |
| `@SpringBootTest` | The whole application context |
| `webEnvironment = RANDOM_PORT` | Start a real server on a free port |
| `@AutoConfigureMockMvc` / `@AutoConfigureRestTestClient` | Inject a ready `MockMvc` / `RestTestClient` |
| `@ServiceConnection` | Point the app's connection (datasource) at a Testcontainers container |
| `@Import(...)` | Add test configuration to the context |

## Troubleshooting during the session

| Symptom | Fix |
|---------|-----|
| "Could not find a valid Docker environment" | Docker isn't running: `docker info` |
| First test run is very slow | It's pulling `postgres:17`; do `docker pull postgres:17` beforehand |
| Tests pass alone, fail in the full suite | Shared data between tests: clean up in `@BeforeEach`, or use unique data |
| The app starts several times during `./mvnw verify` | Test classes with different configurations: see `test-06` |
| Test annotation not found after copying from a tutorial | Spring Boot 4 moved test support into per-technology modules (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jdbc-test`, `spring-boot-resttestclient`) with new packages |
