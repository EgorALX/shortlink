package com.shortlink.core.service;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeGeneratorTest {

    @Test
    void codesHaveRightLengthAndOnlyLettersAndDigits() {
        CodeGenerator generator = new CodeGenerator(new Random(), 6);

        String code = generator.generate();

        assertEquals(6, code.length());
        assertTrue(code.matches("[0-9a-zA-Z]+"));
    }
}
