package com.shortlink.core.service;

import com.shortlink.core.AppConfig;
import com.shortlink.core.Messages;
import com.shortlink.core.model.Link;
import com.shortlink.core.model.OpenResult;
import com.shortlink.core.storage.LinkStorage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LinkService {

    private static final int MAX_URL_LENGTH = 2048;

    private final LinkStorage storage;
    private final CodeGenerator codeGenerator;
    private final NotificationService notificationService;
    private final AppConfig config;
    private final Map<String, OpenResult.Status> retiredCodes = new HashMap<>();
    private final Browser browser;
    private Clock clock;

    public LinkService(LinkStorage storage, CodeGenerator codeGenerator, NotificationService notificationService,
                       AppConfig config, Clock clock, Browser browser) {
        this.storage = storage;
        this.codeGenerator = codeGenerator;
        this.notificationService = notificationService;
        this.config = config;
        this.clock = clock;
        this.browser = browser;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public String validateUrl(String url) {
        if (url.isEmpty()) {
            return Messages.URL_EMPTY;
        }
        if (url.length() > MAX_URL_LENGTH) {
            return String.format(Messages.URL_TOO_LONG, MAX_URL_LENGTH);
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return Messages.URL_INVALID;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return Messages.URL_SCHEME;
        }
        if (uri.getHost() == null) {
            return Messages.URL_INVALID;
        }
        return null;
    }

    public String validateLimit(int limit) {
        if (limit < 1 || limit > config.getMaxClickLimit()) {
            return String.format(Messages.LIMIT_INVALID, config.getMaxClickLimit());
        }
        return null;
    }

    public Link create(UUID ownerId, String url, int clickLimit) {
        String code = codeGenerator.generate();
        while (storage.exists(code) || retiredCodes.containsKey(code)) {
            code = codeGenerator.generate();
        }
        Instant now = clock.instant();
        Link link = new Link(code, url, ownerId, clickLimit, now, now.plus(config.getTtl()));
        storage.save(link);
        return link;
    }

    public OpenResult open(String shortLink) throws IOException {
        String code = extractCode(shortLink);
        Link link = find(code);
        if (link == null) {
            return new OpenResult(retiredCodes.getOrDefault(code, OpenResult.Status.NOT_FOUND), null);
        }
        if (link.isLimitReached()) {
            return new OpenResult(OpenResult.Status.LIMIT_REACHED, link);
        }
        browser.open(link.getOriginalUrl());
        link.registerClick();
        if (link.isLimitReached()) {
            String message = String.format(Messages.NOTIFY_LIMIT_REACHED, toShortLink(link), link.getClicks(),
                    link.getClickLimit());
            notificationService.notify(link.getOwnerId(), message);
        }
        return new OpenResult(OpenResult.Status.OPENED, link);
    }

    public String editLimit(UUID userId, String shortLink, int newLimit) {
        Link link = find(shortLink);
        String error = checkOwner(link, userId, shortLink);
        if (error != null) {
            return error;
        }
        error = validateLimit(newLimit);
        if (error != null) {
            return error;
        }
        if (newLimit <= link.getClicks()) {
            return String.format(Messages.LIMIT_BELOW_CLICKS, link.getClicks());
        }
        link.setClickLimit(newLimit);
        return null;
    }

    public String editUrl(UUID userId, String shortLink, String newUrl) {
        Link link = find(shortLink);
        String error = checkOwner(link, userId, shortLink);
        if (error != null) {
            return error;
        }
        error = validateUrl(newUrl);
        if (error != null) {
            return error;
        }
        if (link.getOriginalUrl().equals(newUrl)) {
            return Messages.SAME_URL;
        }
        link.setOriginalUrl(newUrl);
        return null;
    }

    public String delete(UUID userId, String shortLink) {
        Link link = find(shortLink);
        String error = checkOwner(link, userId, shortLink);
        if (error != null) {
            return error;
        }
        storage.delete(link.getCode());
        retiredCodes.put(link.getCode(), OpenResult.Status.NOT_FOUND);
        return null;
    }

    public String checkOwner(Link link, UUID userId, String shortLink) {
        if (link == null) {
            return String.format(Messages.LINK_NOT_FOUND, shortLink);
        }
        if (!link.isOwnedBy(userId)) {
            return Messages.NOT_OWNER;
        }
        return null;
    }

    public Link find(String shortLink) {
        Link link = storage.find(extractCode(shortLink));
        if (link != null && link.isExpired(clock.instant())) {
            removeExpiredLink(link);
            return null;
        }
        return link;
    }

    public List<Link> getLinks(UUID ownerId) {
        removeExpired();
        return storage.findByOwner(ownerId);
    }

    public List<Link> removeExpired() {
        Instant now = clock.instant();
        List<Link> expired = new ArrayList<>();
        for (Link link : storage.findAll()) {
            if (link.isExpired(now)) {
                removeExpiredLink(link);
                expired.add(link);
            }
        }
        return expired;
    }

    public String toShortLink(Link link) {
        return config.getDomain() + "/" + link.getCode();
    }

    public boolean isShortLink(String text) {
        return text.startsWith(config.getDomain() + "/");
    }

    public int getDefaultClickLimit() {
        return config.getDefaultClickLimit();
    }

    private void removeExpiredLink(Link link) {
        storage.delete(link.getCode());
        retiredCodes.put(link.getCode(), OpenResult.Status.EXPIRED);
        notificationService.notify(link.getOwnerId(), String.format(Messages.NOTIFY_EXPIRED, toShortLink(link)));
    }

    private String extractCode(String shortLink) {
        String link = shortLink.trim();
        int slash = link.lastIndexOf('/');
        if (slash >= 0) {
            return link.substring(slash + 1);
        }
        return link;
    }
}
