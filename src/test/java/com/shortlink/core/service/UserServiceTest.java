package com.shortlink.core.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private final UserService userService = new UserService();

    @Test
    void uuidIsCreatedOnFirstRequestOnly() {
        assertFalse(userService.hasCurrentUser());

        UUID first = userService.getOrCreateCurrentUser();

        assertTrue(userService.hasCurrentUser());
        assertEquals(first, userService.getOrCreateCurrentUser());
    }

    @Test
    void newUserGetsAnotherUuid() {
        UUID first = userService.getOrCreateCurrentUser();

        userService.startNewUser();

        assertFalse(userService.hasCurrentUser());
        assertNotEquals(first, userService.getOrCreateCurrentUser());
    }

    @Test
    void switchToKnownUser() {
        UUID first = userService.getOrCreateCurrentUser();
        userService.startNewUser();
        userService.getOrCreateCurrentUser();

        assertNull(userService.switchTo(first.toString()));
        assertEquals(first, userService.getCurrentUser());
    }

    @Test
    void switchToUnknownOrInvalidUuidFails() {
        assertNotNull(userService.switchTo(UUID.randomUUID().toString()));
        assertNotNull(userService.switchTo("not-a-uuid"));
        assertFalse(userService.hasCurrentUser());
    }
}
