package com.kood.rentaltracker.security;

import com.password4j.Argon2Function;
import com.password4j.Password;
import com.password4j.types.Argon2;

public class PasswordHasher {

    private final Argon2Function argon2;

    public PasswordHasher() {
        this.argon2 = Argon2Function.getInstance(
                1024,
                3,
                2,
                32,
                Argon2.ID,
                19
        );
    }

    public String hash(String password) {
        return Password
                .hash(password)
                .with(argon2)
                .getResult();
    }

    public boolean matches(
            String password,
            String passwordHash
    ) {
        return Password
                .check(password, passwordHash)
                .with(argon2);
    }
}
