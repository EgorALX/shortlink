package com.shortlink.core.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceTest {

    @Test
    void messagesAreDeliveredOnlyToTheirUserAndOnce() {
        NotificationService notificationService = new NotificationService();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        notificationService.notify(first, "раз");
        notificationService.notify(first, "два");

        assertTrue(notificationService.takeAll(second).isEmpty());
        assertEquals(List.of("раз", "два"), notificationService.takeAll(first));
        assertTrue(notificationService.takeAll(first).isEmpty());
    }
}
