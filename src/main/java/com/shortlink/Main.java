package com.shortlink;

import com.shortlink.cli.ConsoleInputHandler;
import com.shortlink.cli.InputReader;
import com.shortlink.cli.LinkCommands;
import com.shortlink.core.AppConfig;
import com.shortlink.core.service.CodeGenerator;
import com.shortlink.core.service.LinkService;
import com.shortlink.core.service.NotificationService;
import com.shortlink.core.service.UserService;
import com.shortlink.core.storage.LinkStorage;
import com.shortlink.infra.ConfigLoader;
import com.shortlink.infra.DesktopBrowser;
import com.shortlink.infra.InMemoryLinkStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Random;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        AppConfig config;
        try {
            config = new ConfigLoader().load(Path.of("application.properties"));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Не удалось прочитать настройки: " + e.getMessage());
            return;
        }

        LinkStorage storage = new InMemoryLinkStorage();
        NotificationService notificationService = new NotificationService();
        CodeGenerator codeGenerator = new CodeGenerator(new Random(), config.getCodeLength());
        LinkService linkService = new LinkService(storage, codeGenerator, notificationService, config,
                Clock.systemDefaultZone(), new DesktopBrowser());
        UserService userService = new UserService();

        InputReader input = new InputReader(new Scanner(System.in));
        LinkCommands linkCommands = new LinkCommands(linkService, userService, notificationService, input);

        new ConsoleInputHandler(input, linkService, userService, linkCommands).readInput();
    }
}
