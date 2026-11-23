package com.shortlink.core.service;

import java.util.Random;

public class CodeGenerator {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final Random random;
    private final int length;

    public CodeGenerator(Random random, int length) {
        this.random = random;
        this.length = length;
    }

    public String generate() {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
