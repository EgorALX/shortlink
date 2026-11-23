package com.shortlink.cli;

import com.shortlink.core.Messages;

import java.util.Scanner;

public class InputReader {

    private final Scanner scanner;

    public InputReader(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine() {
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }

    public String readText(String prompt) {
        System.out.print(prompt);
        String input = readLine();
        if (input == null) {
            return null;
        }
        if (input.equalsIgnoreCase("cancel")) {
            System.out.println("Отменено");
            return null;
        }
        return input;
    }

    public boolean askYesNo(String question) {
        while (true) {
            String answer = readText(question);
            if (answer == null) {
                return false;
            }
            answer = answer.toLowerCase();
            if (answer.equals("да") || answer.equals("yes") || answer.equals("y")) {
                return true;
            }
            if (answer.equals("нет") || answer.equals("no") || answer.equals("n")) {
                return false;
            }
            System.out.println(Messages.ANSWER_YES_NO);
        }
    }
}
