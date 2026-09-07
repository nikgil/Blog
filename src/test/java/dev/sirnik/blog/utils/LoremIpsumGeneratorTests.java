package dev.sirnik.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.sirnik.blog.utils.testing.LoremIpsumGenerator;

class LoremIpsumGeneratorTests {

    @Test
    void returnsRequestedNumberOfWords() {
        assertThat(LoremIpsumGenerator.getWords(5))
            .isEqualTo("Lorem ipsum dolor sit amet,");
    }

    @Test
    void cyclesWordsWhenAmountExceedsSource() {
        String[] words = LoremIpsumGenerator.getWords(490).split(" ");

        assertThat(words).hasSize(490);
        assertThat(words[488]).isEqualTo(words[0]);
        assertThat(words[489]).isEqualTo(words[1]);
    }

    @Test
    void cyclesParagraphsWhenAmountExceedsSource() {
        String[] paragraphs = LoremIpsumGenerator
            .getParagraphs(7)
            .split("\\R\\R");

        assertThat(paragraphs).hasSize(7);
        assertThat(paragraphs[5]).isEqualTo(paragraphs[0]);
        assertThat(paragraphs[6]).isEqualTo(paragraphs[1]);
    }

    @Test
    void returnsEmptyTextForZeroAmount() {
        assertThat(LoremIpsumGenerator.getWords(0)).isEmpty();
        assertThat(LoremIpsumGenerator.getParagraphs(0)).isEmpty();
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThatThrownBy(() -> LoremIpsumGenerator.getWords(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("amount must be non-negative");
        assertThatThrownBy(() -> LoremIpsumGenerator.getParagraphs(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("amount must be non-negative");
    }
}
