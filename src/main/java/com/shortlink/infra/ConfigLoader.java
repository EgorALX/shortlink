package com.shortlink.infra;

import com.shortlink.core.AppConfig;
import com.shortlink.core.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class ConfigLoader {

    private static final String RESOURCE = "/application.properties";

    public AppConfig load(Path file) throws IOException {
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } else {
            try (InputStream stream = ConfigLoader.class.getResourceAsStream(RESOURCE)) {
                if (stream == null) {
                    throw new IOException(String.format(Messages.CONFIG_NOT_FOUND, RESOURCE));
                }
                properties.load(stream);
            }
        }
        return parse(properties);
    }

    public AppConfig parse(Properties properties) {
        String domain = getString(properties, "link.domain");
        int codeLength = getInt(properties, "link.code.length", 4, 12);
        int ttlMinutes = getInt(properties, "link.ttl.minutes", 1, 525600);
        int maxLimit = getInt(properties, "link.click.limit.max", 1, 1000000);
        int defaultLimit = getInt(properties, "link.click.limit.default", 1, maxLimit);
        return new AppConfig(domain, codeLength, Duration.ofMinutes(ttlMinutes), defaultLimit, maxLimit);
    }

    private String getString(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(String.format(Messages.CONFIG_MISSING, key));
        }
        return value.trim();
    }

    private int getInt(Properties properties, String key, int min, int max) {
        String value = getString(properties, key);
        try {
            int number = Integer.parseInt(value);
            if (number < min || number > max) {
                throw new IllegalArgumentException(String.format(Messages.CONFIG_RANGE, key, value, min, max));
            }
            return number;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(Messages.CONFIG_INVALID, key, value));
        }
    }
}
