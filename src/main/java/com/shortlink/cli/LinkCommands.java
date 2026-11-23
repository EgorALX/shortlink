package com.shortlink.cli;

import com.shortlink.core.Messages;
import com.shortlink.core.model.Link;
import com.shortlink.core.model.OpenResult;
import com.shortlink.core.service.LinkService;
import com.shortlink.core.service.NotificationService;
import com.shortlink.core.service.UserService;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class LinkCommands {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final String TABLE_ROW = "%-16s %-42s %-9s %-17s %s%n";
    private static final int MAX_URL_IN_TABLE = 42;

    private final LinkService linkService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final InputReader input;

    public LinkCommands(LinkService linkService, UserService userService, NotificationService notificationService,
                        InputReader input) {
        this.linkService = linkService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.input = input;
    }

    public void create() {
        String url = readUrl("Ссылка: ");
        if (url == null) {
            return;
        }
        int defaultLimit = linkService.getDefaultClickLimit();
        Integer limit = readLimit("Лимит переходов: ", defaultLimit);
        if (limit == null) {
            return;
        }
        boolean newUser = !userService.hasCurrentUser();
        UUID userId = userService.getOrCreateCurrentUser();
        Link link = linkService.create(userId, url, limit);
        if (newUser) {
            System.out.println("Ваш UUID: " + userId);
            System.out.println("Сохраните его: чтобы вернуться к своим ссылкам, введите user и этот UUID");
        }
        System.out.println("Короткая ссылка: " + linkService.toShortLink(link));
        System.out.println("Лимит переходов: " + limit + ", действует до " + formatTime(link.getExpiresAt()));
    }

    public void open(String shortLink) {
        if (shortLink.isEmpty()) {
            shortLink = readShortLink();
        }
        if (shortLink == null) {
            return;
        }
        OpenResult result;
        try {
            result = linkService.open(shortLink);
        } catch (IOException e) {
            System.out.println("Не удалось открыть браузер: " + e.getMessage() + ". Можно повторить переход.");
            return;
        }
        switch (result.getStatus()) {
            case NOT_FOUND -> System.out.println(String.format(Messages.LINK_NOT_FOUND, shortLink));
            case EXPIRED -> System.out.println(String.format(Messages.LINK_EXPIRED, shortLink));
            case LIMIT_REACHED -> System.out.println(String.format(Messages.LINK_LIMIT_REACHED, shortLink));
            case OPENED -> printOpened(result.getLink());
        }
    }

    public void list() {
        List<Link> links = linkService.getLinks(userService.getCurrentUser());
        if (links.isEmpty()) {
            System.out.println(Messages.NO_LINKS);
            return;
        }
        System.out.printf(TABLE_ROW, "Короткая ссылка", "Исходный адрес", "Переходы", "Действует до", "Статус");
        for (Link link : links) {
            String status = link.isLimitReached() ? "лимит исчерпан" : "активна";
            System.out.printf(TABLE_ROW, linkService.toShortLink(link), shorten(link.getOriginalUrl()),
                    link.getClicks() + "/" + link.getClickLimit(), formatTime(link.getExpiresAt()), status);
        }
    }

    public void edit(String action) {
        switch (action) {
            case "limit" -> editLimit();
            case "url" -> editUrl();
            case "" -> System.out.println(Messages.EDIT_ACTION_REQUIRED);
            default -> System.out.println(String.format(Messages.UNKNOWN_ACTION, action, "edit"));
        }
    }

    public void delete() {
        Link link = readOwnLink();
        if (link == null) {
            return;
        }
        String shortLink = linkService.toShortLink(link);
        if (!input.askYesNo("Удалить ссылку " + shortLink + "? (да/нет): ")) {
            return;
        }
        String error = linkService.delete(userService.getCurrentUser(), link.getCode());
        if (error != null) {
            System.out.println(error);
        } else {
            System.out.println("Ссылка " + shortLink + " удалена");
        }
    }

    public void user(String argument) {
        if (argument.isEmpty()) {
            if (userService.hasCurrentUser()) {
                System.out.println("Ваш UUID: " + userService.getCurrentUser());
            } else {
                System.out.println("UUID появится после создания первой ссылки командой create");
            }
        } else if (argument.equals("new")) {
            userService.startNewUser();
            System.out.println("Вы начали как новый пользователь. UUID появится после создания первой ссылки");
        } else {
            String error = userService.switchTo(argument);
            if (error != null) {
                System.out.println(error);
            } else {
                System.out.println("Текущий пользователь: " + userService.getCurrentUser());
            }
        }
    }

    public void printNotifications() {
        if (!userService.hasCurrentUser()) {
            return;
        }
        for (String message : notificationService.takeAll(userService.getCurrentUser())) {
            System.out.println("УВЕДОМЛЕНИЕ: " + message);
        }
    }

    private void editLimit() {
        Link link = readOwnLink();
        if (link == null) {
            return;
        }
        String prompt = "Новый лимит (сейчас " + link.getClickLimit() + ", переходов " + link.getClicks() + "): ";
        do {
            Integer limit = readLimit(prompt, null);
            if (limit == null) {
                return;
            }
            String error = linkService.editLimit(userService.getCurrentUser(), link.getCode(), limit);
            if (error == null) {
                System.out.println("Лимит ссылки " + linkService.toShortLink(link) + " изменён: " + limit);
                return;
            }
            System.out.println(error);
        } while (linkService.find(link.getCode()) != null);
    }

    private void editUrl() {
        Link link = readOwnLink();
        if (link == null) {
            return;
        }
        do {
            String url = input.readText("Новый адрес: ");
            if (url == null) {
                return;
            }
            String error = linkService.editUrl(userService.getCurrentUser(), link.getCode(), url);
            if (error == null) {
                System.out.println("Ссылка " + linkService.toShortLink(link) + " теперь ведёт на " + url);
                return;
            }
            System.out.println(error);
        } while (linkService.find(link.getCode()) != null);
    }

    private String readUrl(String prompt) {
        while (true) {
            String url = input.readText(prompt);
            if (url == null) {
                return null;
            }
            String error = linkService.validateUrl(url);
            if (error == null) {
                return url;
            }
            System.out.println(error);
        }
    }

    private Integer readLimit(String prompt, Integer defaultLimit) {
        while (true) {
            String text = input.readText(prompt);
            if (text == null) {
                return null;
            }
            if (text.isEmpty() && defaultLimit != null) {
                return defaultLimit;
            }
            int limit;
            try {
                limit = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println(Messages.NOT_A_NUMBER);
                continue;
            }
            String error = linkService.validateLimit(limit);
            if (error == null) {
                return limit;
            }
            System.out.println(error);
        }
    }

    private String readShortLink() {
        while (true) {
            String shortLink = input.readText("Короткая ссылка: ");
            if (shortLink == null) {
                return null;
            }
            if (!shortLink.isEmpty()) {
                return shortLink;
            }
            System.out.println(Messages.SHORT_LINK_EMPTY);
        }
    }

    private Link readOwnLink() {
        String shortLink = readShortLink();
        if (shortLink == null) {
            return null;
        }
        Link link = linkService.find(shortLink);
        String error = linkService.checkOwner(link, userService.getCurrentUser(), shortLink);
        if (error != null) {
            System.out.println(error);
            return null;
        }
        return link;
    }

    private void printOpened(Link link) {
        String url = link.getOriginalUrl();
        System.out.println("Переход на " + url);
        int left = link.getClickLimit() - link.getClicks();
        if (left == 0) {
            System.out.println("Это был последний доступный переход по ссылке");
        } else {
            System.out.println("Осталось переходов: " + left);
        }
    }

    private String formatTime(Instant time) {
        return DATE_TIME_FORMAT.format(time);
    }

    private String shorten(String url) {
        if (url.length() <= MAX_URL_IN_TABLE) {
            return url;
        }
        return url.substring(0, MAX_URL_IN_TABLE - 3) + "...";
    }
}
