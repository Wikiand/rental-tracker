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
    void rejectsBlankUsername() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createOwner(" ")
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
            return Optional.ofNullable(user);
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