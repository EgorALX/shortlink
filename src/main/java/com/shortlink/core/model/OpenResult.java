package com.shortlink.core.model;

public class OpenResult {

    public enum Status {
        OPENED,
        NOT_FOUND,
        EXPIRED,
        LIMIT_REACHED
    }

    private final Status status;
    private final Link link;

    public OpenResult(Status status, Link link) {
        this.status = status;
        this.link = link;
    }

    public Status getStatus() {
        return status;
    }

    public Link getLink() {
        return link;
    }
}
