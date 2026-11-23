package com.shortlink.core.service;

import com.shortlink.core.Messages;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UserService {

    private final Set<UUID> users = new HashSet<>();
    private UUID currentUser;

    public UUID getCurrentUser() {
        return currentUser;
    }

    public boolean hasCurrentUser() {
        return currentUser != null;
    }

    public UUID getOrCreateCurrentUser() {
        if (currentUser == null) {
            currentUser = UUID.randomUUID();
            users.add(currentUser);
        }
        return currentUser;
    }

    public String switchTo(String uuid) {
        UUID userId;
        try {
            userId = UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return Messages.UUID_INVALID;
        }
        if (!users.contains(userId)) {
            return String.format(Messages.USER_NOT_FOUND, uuid);
        }
        currentUser = userId;
        return null;
    }

    public void startNewUser() {
        currentUser = null;
    }
}
