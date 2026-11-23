package com.shortlink.core.model;

import java.time.Instant;
import java.util.UUID;

public class Link {

    private final String code;
    private final UUID ownerId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private String originalUrl;
    private int clickLimit;
    private int clicks;

    public Link(String code, String originalUrl, UUID ownerId, int clickLimit, Instant createdAt, Instant expiresAt) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.ownerId = ownerId;
        this.clickLimit = clickLimit;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getCode() {
        return code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public int getClickLimit() {
        return clickLimit;
    }

    public void setClickLimit(int clickLimit) {
        this.clickLimit = clickLimit;
    }

    public int getClicks() {
        return clicks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void registerClick() {
        clicks++;
    }

    public boolean isLimitReached() {
        return clicks >= clickLimit;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId.equals(userId);
    }
}
