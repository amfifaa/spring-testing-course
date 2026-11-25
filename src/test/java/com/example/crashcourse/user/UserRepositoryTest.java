package com.example.crashcourse.user;

import com.example.crashcourse.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// "Slice" test for the data layer: starts ONLY Spring Data JDBC, the DataSource and Liquibase.
// No web layer, no services. It runs against a REAL Postgres (Testcontainers), so the SQL,
// the Liquibase migrations and the database constraints are the real thing.
// Each test runs in a transaction that is ROLLED BACK at the end, so tests can't see each other's data.
@DataJdbcTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    // The Postgres container is shared with the integration tests (see TestcontainersConfiguration),
    // and THEY don't roll back. So start from an empty table; this delete is rolled back too.
    @BeforeEach
    void startWithAnEmptyTable() {
        userRepository.deleteAll();
    }

    @Test
    void saveInsertsANewUserAndTheDatabaseAssignsTheId() {
        User saved = userRepository.save(new User(null, "ada", "ada@example.com", "Ada Lovelace"));

        assertThat(saved.id()).isNotNull();
        assertThat(userRepository.findById(saved.id())).contains(saved);
    }

    @Test
    void findByEmailFindsTheMatchingUser() {
        userRepository.save(new User(null, "ada", "ada@example.com", null));
        userRepository.save(new User(null, "linus", "linus@example.com", null));

        assertThat(userRepository.findByEmail("linus@example.com"))
                .get().extracting(User::username).isEqualTo("linus");
    }

    @Test
    void findByEmailIsEmptyWhenNobodyHasThatEmail() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void theDatabaseRejectsADuplicateEmail() {
        // The unique constraint lives in the Liquibase changeset, not in Java.
        // A mock could never tell us this; only the real database can.
        userRepository.save(new User(null, "ada", "ada@example.com", null));

        // Spring translates the Postgres error into its own DuplicateKeyException (a DataAccessException),
        // so the test doesn't depend on the database vendor's exception classes.
        assertThatThrownBy(() -> userRepository.save(new User(null, "ada2", "ada@example.com", null)))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("users_email_key");
    }

    @Test
    void eachTestStartsWithAnEmptyTable() {
        // Passes in any order: this class's inserts are rolled back, and @BeforeEach clears
        // whatever the integration tests left in the shared database.
        assertThat(userRepository.count()).isZero();
    }
}
