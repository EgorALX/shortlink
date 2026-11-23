package com.shortlink.infra;

import com.shortlink.core.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    private final ConfigLoader loader = new ConfigLoader();

    @Test
    void configIsReadFromFile() throws Exception {
        Path file = tempDir.resolve("application.properties");
        Files.write(file, List.of(
                "link.domain=sho.rt",
                "link.code.length=8",
                "link.ttl.minutes=60",
                "link.click.limit.default=3",
                "link.click.limit.max=50"));

        AppConfig config = loader.load(file);

        assertEquals("sho.rt", config.getDomain());
        assertEquals(8, config.getCodeLength());
        assertEquals(Duration.ofHours(1), config.getTtl());
        assertEquals(3, config.getDefaultClickLimit());
        assertEquals(50, config.getMaxClickLimit());
    }

    @Test
    void defaultConfigIsUsedWhenFileIsMissing() throws Exception {
        AppConfig config = loader.load(tempDir.resolve("missing.properties"));

        assertEquals("clck.ru", config.getDomain());
        assertEquals(Duration.ofDays(1), config.getTtl());
    }

    @Test
    void wrongValuesAreRejected() throws Exception {
        Path file = tempDir.resolve("application.properties");
        Files.write(file, List.of(
                "link.domain=clck.ru",
                "link.code.length=6",
                "link.ttl.minutes=сутки",
                "link.click.limit.default=10",
                "link.click.limit.max=100"));

        assertThrows(IllegalArgumentException.class, () -> loader.load(file));
    }

    @Test
    void defaultLimitCannotBeGreaterThanMax() throws Exception {
        Path file = tempDir.resolve("application.properties");
        Files.write(file, List.of(
                "link.domain=clck.ru",
                "link.code.length=6",
                "link.ttl.minutes=60",
                "link.click.limit.default=500",
                "link.click.limit.max=100"));

        assertThrows(IllegalArgumentException.class, () -> loader.load(file));
    }

    @Test
    void missingParameterIsRejected() throws Exception {
        Path file = tempDir.resolve("application.properties");
        Files.write(file, List.of("link.domain=clck.ru"));

        assertThrows(IllegalArgumentException.class, () -> loader.load(file));
    }
}
