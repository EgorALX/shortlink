package com.shortlink.core.service;

import com.shortlink.core.AppConfig;
import com.shortlink.core.model.Link;
import com.shortlink.core.model.OpenResult;
import com.shortlink.infra.InMemoryLinkStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkServiceTest {

    private final List<String> openedUrls = new ArrayList<>();
    private static final String URL = "https://www.baeldung.com/java-9-http-client";

    private final UUID owner = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();

    private InMemoryLinkStorage storage;
    private NotificationService notificationService;
    private Instant now;
    private LinkService linkService;

    @BeforeEach
    void setUp() {
        storage = new InMemoryLinkStorage();
        notificationService = new NotificationService();
        now = Instant.parse("2026-11-01T10:00:00Z");
        AppConfig config = new AppConfig("clck.ru", 6, Duration.ofDays(1), 10, 1000);
        linkService = new LinkService(storage, new CodeGenerator(new Random(1), 6), notificationService, config,
                Clock.fixed(now, ZoneOffset.UTC), openedUrls::add);
    }

    private void moveTime(Duration duration) {
        now = now.plus(duration);
        linkService.setClock(Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void createdLinkHasDomainCodeAndTtl() {
        Link link = linkService.create(owner, URL, 5);

        assertTrue(linkService.toShortLink(link).startsWith("clck.ru/"));
        assertEquals(6, link.getCode().length());
        assertEquals(Duration.ofDays(1), Duration.between(link.getCreatedAt(), link.getExpiresAt()));
    }

    @Test
    void sameUrlForDifferentUsersGivesDifferentLinks() {
        Link first = linkService.create(owner, URL, 5);
        Link second = linkService.create(stranger, URL, 5);

        assertNotEquals(first.getCode(), second.getCode());
    }

    @Test
    void userCanHaveSeveralLinksAndSeesOnlyHisOwn() {
        linkService.create(owner, URL, 5);
        linkService.create(owner, "https://ru.stackoverflow.com", 5);
        linkService.create(stranger, URL, 5);

        assertEquals(2, linkService.getLinks(owner).size());
        assertEquals(1, linkService.getLinks(stranger).size());
    }

    @Test
    void codesAreReservedWhileActiveAndAfterDeletionOrExpiry() throws IOException {
        CodeGenerator generator = new CodeGenerator(new Random(), 6) {
            private final String[] codes = {"aaaaaa", "aaaaaa", "bbbbbb", "aaaaaa", "bbbbbb", "cccccc"};
            private int index;

            @Override
            public String generate() {
                return codes[index++];
            }
        };
        AppConfig config = new AppConfig("clck.ru", 6, Duration.ofDays(1), 10, 1000);
        linkService = new LinkService(storage, generator, notificationService, config,
                Clock.fixed(now, ZoneOffset.UTC), openedUrls::add);

        Link deleted = linkService.create(owner, URL, 5);
        Link expired = linkService.create(stranger, URL, 5);
        assertNotEquals(deleted.getCode(), expired.getCode());
        assertNull(linkService.delete(owner, deleted.getCode()));
        moveTime(Duration.ofDays(1));
        linkService.removeExpired();

        Link fresh = linkService.create(stranger, URL, 5);
        assertEquals("cccccc", fresh.getCode());
        assertEquals(OpenResult.Status.NOT_FOUND, linkService.open(deleted.getCode()).getStatus());
        assertEquals(OpenResult.Status.EXPIRED, linkService.open(expired.getCode()).getStatus());
        assertTrue(openedUrls.isEmpty());
    }

    @Test
    void browserFailurePreservesLastClickForRetry() throws IOException {
        Browser browser = url -> {
            openedUrls.add(url);
            if (openedUrls.size() == 1) {
                throw new IOException("Браузер недоступен");
            }
        };
        AppConfig config = new AppConfig("clck.ru", 6, Duration.ofDays(1), 10, 1000);
        linkService = new LinkService(storage, new CodeGenerator(new Random(1), 6), notificationService,
                config, Clock.fixed(now, ZoneOffset.UTC), browser);
        Link link = linkService.create(owner, URL, 1);

        assertThrows(IOException.class, () -> linkService.open(link.getCode()));
        assertEquals(0, link.getClicks());
        assertTrue(notificationService.takeAll(owner).isEmpty());
        assertEquals(OpenResult.Status.OPENED, linkService.open(link.getCode()).getStatus());
        assertEquals(OpenResult.Status.LIMIT_REACHED, linkService.open(link.getCode()).getStatus());
        assertEquals(1, link.getClicks());
        assertEquals(1, notificationService.takeAll(owner).size());
        assertEquals(List.of(URL, URL), openedUrls);
    }

    @Test
    void openCountsClicksAndAcceptsFullLinkOrCode() throws IOException {
        Link link = linkService.create(owner, URL, 5);

        assertEquals(OpenResult.Status.OPENED, linkService.open(linkService.toShortLink(link)).getStatus());
        assertEquals(OpenResult.Status.OPENED, linkService.open(link.getCode()).getStatus());
        assertEquals(2, link.getClicks());
        assertEquals(List.of(URL, URL), openedUrls);
    }

    @Test
    void openIsBlockedWhenLimitReached() throws IOException {
        Link link = linkService.create(owner, URL, 2);
        linkService.open(link.getCode());
        linkService.open(link.getCode());

        OpenResult result = linkService.open(link.getCode());

        assertEquals(OpenResult.Status.LIMIT_REACHED, result.getStatus());
        assertEquals(2, link.getClicks());
    }

    @Test
    void ownerIsNotifiedWhenLimitReached() throws IOException {
        Link link = linkService.create(owner, URL, 1);

        linkService.open(link.getCode());

        List<String> messages = notificationService.takeAll(owner);
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("исчерпан"));
    }

    @Test
    void shortLinkIsRecognizedByDomain() {
        assertTrue(linkService.isShortLink("clck.ru/3DZHeG"));
        assertFalse(linkService.isShortLink("create"));
        assertFalse(linkService.isShortLink("https://www.baeldung.com"));
    }

    @Test
    void unknownLinkIsNotFound() throws IOException {
        assertEquals(OpenResult.Status.NOT_FOUND, linkService.open("clck.ru/nope00").getStatus());
    }

    @Test
    void linkExpiresAfterTtl() throws IOException {
        Link link = linkService.create(owner, URL, 5);

        moveTime(Duration.ofHours(23));
        assertEquals(OpenResult.Status.OPENED, linkService.open(link.getCode()).getStatus());

        moveTime(Duration.ofHours(1));
        assertEquals(OpenResult.Status.EXPIRED, linkService.open(link.getCode()).getStatus());
        assertNull(storage.find(link.getCode()));
        assertTrue(notificationService.takeAll(owner).get(0).contains("истёк"));
    }

    @Test
    void removeExpiredDeletesOnlyExpiredLinks() {
        Link old = linkService.create(owner, URL, 5);
        moveTime(Duration.ofHours(12));
        Link fresh = linkService.create(owner, URL, 5);
        moveTime(Duration.ofHours(12));

        List<Link> removed = linkService.removeExpired();

        assertEquals(List.of(old), removed);
        assertNull(storage.find(old.getCode()));
        assertNotNull(storage.find(fresh.getCode()));
    }

    @Test
    void openAfterAutoRemovalSaysExpired() throws IOException {
        Link link = linkService.create(owner, URL, 5);
        moveTime(Duration.ofDays(1));
        linkService.removeExpired();

        assertEquals(OpenResult.Status.EXPIRED, linkService.open(link.getCode()).getStatus());
    }

    @Test
    void validateUrl() {
        assertNotNull(linkService.validateUrl(""));
        assertNotNull(linkService.validateUrl("abc"));
        assertNotNull(linkService.validateUrl("ftp://example.com"));
        assertNotNull(linkService.validateUrl("https://"));
        assertNotNull(linkService.validateUrl("https://exa mple.com"));
        assertNotNull(linkService.validateUrl("https://example.com/" + "a".repeat(2100)));
        assertNull(linkService.validateUrl(URL));
        assertNull(linkService.validateUrl("http://localhost:8080/test?x=1"));
    }

    @Test
    void validateLimit() {
        assertNotNull(linkService.validateLimit(0));
        assertNotNull(linkService.validateLimit(1001));
        assertNull(linkService.validateLimit(1));
        assertNull(linkService.validateLimit(1000));
    }

    @Test
    void onlyOwnerCanEditLimit() throws IOException {
        Link link = linkService.create(owner, URL, 5);
        linkService.open(link.getCode());
        linkService.open(link.getCode());

        assertNotNull(linkService.editLimit(stranger, link.getCode(), 10));
        assertNotNull(linkService.editLimit(owner, link.getCode(), 2));
        assertNotNull(linkService.editLimit(owner, link.getCode(), 5000));
        assertNotNull(linkService.editLimit(owner, "clck.ru/nope00", 10));
        assertNull(linkService.editLimit(owner, link.getCode(), 3));
        assertEquals(3, link.getClickLimit());
    }

    @Test
    void raisingLimitUnblocksLink() throws IOException {
        Link link = linkService.create(owner, URL, 1);
        linkService.open(link.getCode());

        linkService.editLimit(owner, link.getCode(), 2);

        assertEquals(OpenResult.Status.OPENED, linkService.open(link.getCode()).getStatus());
    }

    @Test
    void onlyOwnerCanEditUrl() {
        Link link = linkService.create(owner, URL, 5);

        assertNotNull(linkService.editUrl(stranger, link.getCode(), "https://ru.stackoverflow.com"));
        assertNotNull(linkService.editUrl(owner, link.getCode(), "not a url"));
        assertNotNull(linkService.editUrl(owner, link.getCode(), URL));
        assertNull(linkService.editUrl(owner, link.getCode(), "https://ru.stackoverflow.com"));
        assertEquals("https://ru.stackoverflow.com", link.getOriginalUrl());
    }

    @Test
    void onlyOwnerCanDelete() {
        Link link = linkService.create(owner, URL, 5);

        assertNotNull(linkService.delete(stranger, link.getCode()));
        assertNotNull(storage.find(link.getCode()));

        assertNull(linkService.delete(owner, linkService.toShortLink(link)));
        assertNull(storage.find(link.getCode()));
    }
}
