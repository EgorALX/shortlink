package com.shortlink.core;

public final class Messages {

    public static final String UNKNOWN_COMMAND = "Неизвестная команда: %s. Введите help для списка команд";
    public static final String UNKNOWN_ACTION = "Неизвестное действие: %s. Подробнее: help %s";
    public static final String USER_REQUIRED =
            "У вас пока нет UUID. Создайте ссылку командой create или введите user и свой UUID";

    public static final String URL_EMPTY = "Ссылка не может быть пустой";
    public static final String URL_TOO_LONG = "Ссылка не должна быть длиннее %d символов";
    public static final String URL_INVALID = "Неверная ссылка. Пример правильной: https://www.baeldung.com/java-9-http-client";
    public static final String URL_SCHEME = "Ссылка должна начинаться с http:// или https://";
    public static final String LIMIT_INVALID = "Лимит переходов должен быть целым числом от 1 до %d";
    public static final String NOT_A_NUMBER = "Введите целое число, например 5";
    public static final String LIMIT_BELOW_CLICKS = "Лимит должен быть больше уже сделанных переходов (%d)";
    public static final String SAME_URL = "Новая ссылка совпадает с текущей";

    public static final String SHORT_LINK_EMPTY = "Введите короткую ссылку, например clck.ru/3DZHeG";
    public static final String EDIT_ACTION_REQUIRED = "Укажите, что изменить: edit limit или edit url";
    public static final String LINK_NOT_FOUND = "Ссылка %s не найдена";
    public static final String NOT_OWNER = "Изменять и удалять ссылку может только её владелец";
    public static final String LINK_EXPIRED = "Срок жизни ссылки %s истёк, ссылка удалена";
    public static final String LINK_LIMIT_REACHED = "Лимит переходов по ссылке %s исчерпан, ссылка недоступна";
    public static final String NO_LINKS = "У вас пока нет ссылок";

    public static final String UUID_INVALID = "Неверный формат UUID, пример: 3f1c9a52-6b1e-4f0a-9d7e-2c4b8a1f5e60";
    public static final String USER_NOT_FOUND = "Пользователь %s не найден";
    public static final String ANSWER_YES_NO = "Ответьте да или нет";

    public static final String NOTIFY_LIMIT_REACHED = "Лимит переходов по вашей ссылке %s исчерпан (%d из %d), ссылка больше недоступна";
    public static final String NOTIFY_EXPIRED = "Срок жизни вашей ссылки %s истёк, ссылка удалена";

    public static final String CONFIG_INVALID = "Неверное значение %s в файле настроек: %s";
    public static final String CONFIG_RANGE = "Неверное значение %s в файле настроек: %s, допустимо от %d до %d";
    public static final String CONFIG_NOT_FOUND = "Не найден файл настроек %s";
    public static final String BROWSER_UNAVAILABLE = "браузер на этом компьютере недоступен";
    public static final String BROWSER_BAD_URL = "неверный адрес %s";
    public static final String CONFIG_MISSING = "В файле настроек нет параметра %s";

    private Messages() {
    }
}
