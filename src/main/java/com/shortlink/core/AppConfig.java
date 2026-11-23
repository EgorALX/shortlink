package com.shortlink.core;

import java.time.Duration;

public class AppConfig {

    private final String domain;
    private final int codeLength;
    private final Duration ttl;
    private final int defaultClickLimit;
    private final int maxClickLimit;

    public AppConfig(String domain, int codeLength, Duration ttl, int defaultClickLimit, int maxClickLimit) {
        this.domain = domain;
        this.codeLength = codeLength;
        this.ttl = ttl;
        this.defaultClickLimit = defaultClickLimit;
        this.maxClickLimit = maxClickLimit;
    }

    public String getDomain() {
        return domain;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public Duration getTtl() {
        return ttl;
    }

    public int getDefaultClickLimit() {
        return defaultClickLimit;
    }

    public int getMaxClickLimit() {
        return maxClickLimit;
    }
}
