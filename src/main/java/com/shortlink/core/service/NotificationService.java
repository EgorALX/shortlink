package com.shortlink.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NotificationService {

    private final Map<UUID, List<String>> pending = new HashMap<>();

    public void notify(UUID userId, String message) {
        pending.computeIfAbsent(userId, id -> new ArrayList<>()).add(message);
    }

    public List<String> takeAll(UUID userId) {
        List<String> messages = pending.remove(userId);
        if (messages == null) {
            return new ArrayList<>();
        }
        return messages;
    }
}
