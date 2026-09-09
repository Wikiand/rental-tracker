package com.kood.rentaltracker;

import com.kood.rentaltracker.domain.User;
import com.kood.rentaltracker.repository.UserRepository;
import com.kood.rentaltracker.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    @Test
    void createsOwnerWhenNoOwnerExists() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);

        User owner = service.createOwner("christine");

        assertNotNull(owner);
        assertEquals("christine", owner.getUsername());
        assertEquals(1, owner.getId());
    }

    @Test
    void detectsExistingOwner() {
        FakeUserRepository repository = new FakeUserRepository();
        repository.user = new User(1, "christine");

        UserService service = new UserService(repository);

        assertTrue(service.hasOwner());
        assertEquals(
                "christine",
                service.getOwner().getUsername()
        );
    }

    @Test
    void hasOwnerReturnsFalseWhenNoOwnerExists() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);

        assertFalse(service.hasOwner());
    }

    @Test
    void getOwnerThrowsWhenNoOwnerExists() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);

        assertThrows(
                IllegalStateException.class,
                service::getOwner
        );
    }

    @Test
    void createOwnerReturnsExistingOwner() {
        FakeUserRepository repository = new FakeUserRepository();
        repository.user = new User(1, "existing");

        UserService service = new UserService(repository);

        User owner = service.createOwner("new-owner");

        assertEquals("existing", owner.getUsername());
        assertEquals(1, owner.getId());
    }

    @Test
    void findOrCreateRenterCreatesNewRenter() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);

        User renter = service.findOrCreateRenter(" james ");

        assertNotNull(renter);
        assertEquals("james", renter.getUsername());
        assertEquals(1, renter.getId());
    }

    @Test
    void findOrCreateRenterReturnsExistingRenter() {
        FakeUserRepository repository = new FakeUserRepository();
        repository.user = new User(2, "james");

        UserService service = new UserService(repository);

        User renter = service.findOrCreateRenter(" james ");

        assertEquals("james", renter.getUsername());
        assertEquals(2, renter.getId());
    }

    @Test
    void findByIdReturnsUser() {
        FakeUserRepository repository = new FakeUserRepository();
        repository.user = new User(7, "alice");

        UserService service = new UserService(repository);

        User user = service.findById(7);

        assertEquals(7, user.getId());
        assertEquals("alice", user.getUsername());
    }

    @Test
    void findByIdThrowsWhenUserDoesNotExist() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findById(999)
        );
    }

    @Test
    void rejectsBlankUsername() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createOwner(" ")
        );
    }

    @Test
    void rejectsNullUsernameForOwner() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createOwner(null)
        );
    }

    @Test
    void rejectsBlankUsernameForRenter() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findOrCreateRenter(" ")
        );
    }

    @Test
    void rejectsNullUsernameForRenter() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findOrCreateRenter(null)
        );
    }

    private static class FakeUserRepository implements UserRepository {

        private User user;

        @Override
        public User save(User user) {
            this.user = new User(1, user.getUsername());
            return this.user;
        }

        @Override
        public Optional<User> findById(int id) {
            if (user != null && user.getId() == id) {
                return Optional.of(user);
            }

            return Optional.empty();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            if (user != null
                    && user.getUsername().equals(username)) {
                return Optional.of(user);
            }

            return Optional.empty();
        }

        @Override
        public Optional<User> findOwner() {
            return Optional.ofNullable(user);
        }
    }
}
