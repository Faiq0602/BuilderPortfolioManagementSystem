package com.builder.portfolio.util;

import java.io.Console;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

//* Centralised helper for all console input so that the application does not keep creating new Scanner instances.
// The class also adds gentle validation for numeric inputs and attempts to mask passwords when the environment provides a real console.

public final class ConsoleInput {
    private static final Logger LOGGER = Logger.getLogger(ConsoleInput.class.getName());
    private static final Scanner SCANNER = new Scanner(System.in);

    private ConsoleInput() {
    }

    // Reads a non-null line of text from the user after showing the provided prompt.

    public static String readLine(String prompt) {
        System.out.print(prompt);
        String value = SCANNER.nextLine();
        LOGGER.finest(() -> "Captured text input for prompt: " + prompt);
        return value;
    }

    // Reads an integer from the console, re-prompting the user until a valid number is provided.

    public static int readInt(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid integer input: {0}", input);
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    // Reads a double from the console, ensuring only valid numeric values are accepted.

    public static double readDouble(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Double.parseDouble(input.trim());
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid decimal input: {0}", input);
                System.out.println("Please enter a valid number (decimals are allowed).");
            }
        }
    }

    /**
     * Reads a password while trying to hide the characters typed on the console. When the
     * application runs without a genuine console (for example inside an IDE), the password cannot
     * be hidden; in that case the method warns once and falls back to plain text entry.
     */

    public static String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword("%s", prompt);
            return passwordChars == null ? "" : new String(passwordChars);
        }

        LOGGER.info("Console not available; password input cannot be masked in this environment.");
        return readLine(prompt);
    }


    public static LocalDate readOptionalDate(String prompt) {
        String input = readLine(prompt);
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(input.trim());
        } catch (DateTimeParseException ex) {
            LOGGER.log(Level.WARNING, "Invalid date provided: {0}", input);
            System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            return null;
        }
    }
}
