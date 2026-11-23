package com.shortlink.cli;

import com.shortlink.core.Messages;
import com.shortlink.core.service.LinkService;
import com.shortlink.core.service.UserService;

import java.util.Locale;

public class ConsoleInputHandler {

    private final InputReader input;
    private final LinkService linkService;
    private final UserService userService;
    private final LinkCommands linkCommands;

    public ConsoleInputHandler(InputReader input, LinkService linkService, UserService userService,
                               LinkCommands linkCommands) {
        this.input = input;
        this.linkService = linkService;
        this.userService = userService;
        this.linkCommands = linkCommands;
    }

    public void readInput() {
        System.out.println("Сервис коротких ссылок. Введите help для списка команд");
        while (true) {
            String line = input.readLine();
            if (line == null) {
                break;
            }
            if (line.isEmpty()) {
                continue;
            }
            linkService.removeExpired();
            linkCommands.printNotifications();

            String[] words = line.split("\\s+", 2);
            String command = words[0].toLowerCase(Locale.ROOT);
            String argument = words.length > 1 ? words[1] : "";
            boolean needsUser = command.equals("list") || command.equals("edit") || command.equals("delete");
            if (needsUser && !userService.hasCurrentUser()) {
                System.out.println(Messages.USER_REQUIRED);
                continue;
            }

            switch (command) {
                case "create" -> linkCommands.create();
                case "open" -> linkCommands.open(argument);
                case "list" -> linkCommands.list();
                case "edit" -> linkCommands.edit(argument.toLowerCase(Locale.ROOT));
                case "delete" -> linkCommands.delete();
                case "user" -> linkCommands.user(argument.toLowerCase(Locale.ROOT));
                case "help" -> printHelp(argument.toLowerCase(Locale.ROOT));
                case "exit" -> {
                    System.out.println("Выход из программы");
                    return;
                }
                default -> {
                    if (linkService.isShortLink(words[0])) {
                        linkCommands.open(words[0]);
                    } else {
                        System.out.println(String.format(Messages.UNKNOWN_COMMAND, words[0]));
                    }
                }
            }
            linkCommands.printNotifications();
        }
        System.out.println("Выход из программы");
    }

    private void printHelp(String command) {
        switch (command) {
            case "" -> {
                System.out.println("Команды:");
                System.out.println("  create, open, list, edit, delete  - работа со ссылками");
                System.out.println("  user                             - выбор пользователя");
                System.out.println("  help, exit                       - справка и выход");
                System.out.println("Введите help edit для примера или cancel для отмены ввода.");
            }
            case "create" -> {
                System.out.println("create - создать короткую ссылку.");
                System.out.println("Введите адрес, например https://example.org, затем лимит переходов.");
                System.out.println("Нажмите Enter вместо числа, чтобы взять лимит из настроек.");
            }
            case "open" -> {
                System.out.println("open clck.ru/3DZHeG - открыть ссылку в браузере.");
                System.out.println("Можно ввести open 3DZHeG или просто вставить clck.ru/3DZHeG.");
            }
            case "list" -> System.out.println("list - показать ваши ссылки, переходы и сроки действия.");
            case "edit" -> {
                System.out.println("edit limit - изменить лимит переходов своей ссылки.");
                System.out.println("edit url - изменить исходный адрес своей ссылки.");
            }
            case "delete" -> System.out.println("delete - введите свою короткую ссылку и подтвердите удаление.");
            case "user" -> {
                System.out.println("user - показать свой UUID.");
                System.out.println("user new - начать как новый пользователь.");
                System.out.println("user 3f1c9a52-6b1e-4f0a-9d7e-2c4b8a1f5e60 - переключиться по сохранённому UUID.");
            }
            case "help" -> System.out.println("help - все команды. Введите help edit для справки по команде.");
            case "exit" -> System.out.println("exit - выйти из программы.");
            default -> System.out.println(String.format(Messages.UNKNOWN_COMMAND, command));
        }
    }
}
