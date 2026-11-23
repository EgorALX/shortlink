package com.shortlink.core.service;

import com.shortlink.cli.ConsoleInputHandler;
import com.shortlink.cli.InputReader;
import com.shortlink.cli.LinkCommands;
import com.shortlink.core.AppConfig;
import com.shortlink.core.model.Link;
import com.shortlink.core.model.OpenResult;
import com.shortlink.infra.ConfigLoader;
import com.shortlink.infra.InMemoryLinkStorage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkScenarioTest {

    private final List<String> openedUrls = new ArrayList<>();
    private static final String URL = "https://www.baeldung.com/java-9-http-client";

    private Instant now = Instant.parse("2026-11-01T10:00:00Z");
    private final NotificationService notificationService = new NotificationService();
    private final UserService userService = new UserService();

    private LinkService createService(int ttlMinutes) {
        Properties properties = new Properties();
        properties.setProperty("link.domain", "clck.ru");
        properties.setProperty("link.code.length", "6");
        properties.setProperty("link.ttl.minutes", String.valueOf(ttlMinutes));
        properties.setProperty("link.click.limit.default", "10");
        properties.setProperty("link.click.limit.max", "100");
        AppConfig config = new ConfigLoader().parse(properties);
        return new LinkService(new InMemoryLinkStorage(), new CodeGenerator(new Random(), config.getCodeLength()),
                notificationService, config, Clock.fixed(now, ZoneOffset.UTC), openedUrls::add);
    }

    private void moveTime(LinkService linkService, Duration duration) {
        now = now.plus(duration);
        linkService.setClock(Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void twoUsersShortenSameUrlAndGetDifferentLinks() throws IOException {
        LinkService linkService = createService(1440);
        UUID first = userService.getOrCreateCurrentUser();
        Link firstLink = linkService.create(first, URL, 5);
        userService.startNewUser();
        UUID second = userService.getOrCreateCurrentUser();
        Link secondLink = linkService.create(second, URL, 5);

        assertNotEquals(first, second);
        assertNotEquals(linkService.toShortLink(firstLink), linkService.toShortLink(secondLink));
        assertEquals(URL, linkService.open(linkService.toShortLink(secondLink)).getLink().getOriginalUrl());
    }

    @Test
    void linkIsBlockedAfterLimitAndOwnerIsNotified() throws IOException {
        LinkService linkService = createService(1440);
        UUID owner = userService.getOrCreateCurrentUser();
        Link link = linkService.create(owner, URL, 3);

        for (int i = 0; i < 3; i++) {
            assertEquals(OpenResult.Status.OPENED, linkService.open(linkService.toShortLink(link)).getStatus());
        }

        assertEquals(OpenResult.Status.LIMIT_REACHED, linkService.open(linkService.toShortLink(link)).getStatus());
        assertTrue(notificationService.takeAll(owner).get(0).contains("3 из 3"));
    }

    @Test
    void linkWithHourTtlIsRemovedAfterHour() {
        LinkService linkService = createService(60);
        UUID owner = userService.getOrCreateCurrentUser();
        Link link = linkService.create(owner, URL, 5);

        moveTime(linkService, Duration.ofMinutes(59));
        assertTrue(linkService.removeExpired().isEmpty());

        moveTime(linkService, Duration.ofMinutes(1));
        assertEquals(List.of(link), linkService.removeExpired());
        assertTrue(linkService.getLinks(owner).isEmpty());
        assertTrue(notificationService.takeAll(owner).get(0).contains("истёк"));
    }

    @Test
    void nextCommandRemovesExpiredLinks() {
        LinkService linkService = createService(60);
        UUID owner = userService.getOrCreateCurrentUser();
        linkService.create(owner, URL, 5);
        userService.startNewUser();
        moveTime(linkService, Duration.ofHours(1));

        InputReader input = new InputReader(new Scanner("user\n"));
        LinkCommands commands = new LinkCommands(linkService, userService, notificationService, input);
        new ConsoleInputHandler(input, linkService, userService, commands).readInput();

        assertEquals(1, notificationService.takeAll(owner).size());
        assertTrue(linkService.getLinks(owner).isEmpty());
    }

    @Test
    void linkWithDayTtlLivesWholeDay() throws IOException {
        LinkService linkService = createService(1440);
        UUID owner = userService.getOrCreateCurrentUser();
        Link link = linkService.create(owner, URL, 5);

        moveTime(linkService, Duration.ofHours(23).plusMinutes(59));
        assertTrue(linkService.removeExpired().isEmpty());
        assertEquals(OpenResult.Status.OPENED, linkService.open(link.getCode()).getStatus());

        moveTime(linkService, Duration.ofMinutes(1));
        assertEquals(OpenResult.Status.EXPIRED, linkService.open(link.getCode()).getStatus());
    }

    @Test
    void strangerCannotChangeOrDeleteLink() {
        LinkService linkService = createService(1440);
        UUID owner = userService.getOrCreateCurrentUser();
        Link link = linkService.create(owner, URL, 5);
        userService.startNewUser();
        UUID stranger = userService.getOrCreateCurrentUser();

        assertTrue(linkService.delete(stranger, link.getCode()).contains("владелец"));
        assertTrue(linkService.editLimit(stranger, link.getCode(), 50).contains("владелец"));
        assertEquals(1, linkService.getLinks(owner).size());
    }
}
