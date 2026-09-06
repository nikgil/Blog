package dev.sirnik.blog.utils.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import dev.sirnik.blog.BlogApplication;

public final class LoremIpsumGenerator {

    private static final List<String> PARAGRAPHS = init();
    private static final List<String> WORDS = PARAGRAPHS.stream()
            .flatMap(paragraph -> Arrays.stream(paragraph.split("\\s+")))
            .toList();
    public static final Set<String> UNIQUE_WORDS = Collections
            .unmodifiableSet(new HashSet<>(WORDS));

    // Static class, no construction
    private LoremIpsumGenerator() {
    }

    public static String getParagraphs(int amount) {
        return getParagraphs(amount, false);
    }

    public static String getParagraphs(int amount, boolean shouldFormatHTML) {
        return cycle(PARAGRAPHS, amount, "\n\n", shouldFormatHTML);
    }

    public static String getWords(int amount) {
        return cycle(WORDS, amount, " ", false);
    }

    private static List<String> init() {
        String text;
        try {
            URI resourcePath = BlogApplication.class
                    .getResource("/static/loremipsum.txt").toURI();
            Path path = Paths.get(resourcePath);

            text = Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Could not read classpath resource for Lorem Ipsum", e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid syntax for Lorem Ipsum");
        }

        if (text.isEmpty()) {
            throw new IllegalStateException("Classpath resource is empty");
        }

        return List.of(text.split("\\R\\s*\\R"));
    }

    private static String cycle(List<String> values, int amount,
            String separator, boolean wrapHTML) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }

        StringJoiner result = new StringJoiner(separator);
        for (int index = 0; index < amount; index++) {
            String value = values.get(index % values.size());
            if (wrapHTML) {
                value = "<p>" + value + "</p>";
            }

            result.add(value);
        }
        return result.toString();
    }
}
