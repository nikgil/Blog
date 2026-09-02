package dev.sirnik.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HTMLParserTests {

    @Test
    void returnsFallbackWhenContentHasNoParagraph() {
        String html = """
                <h1>A title is not a preview</h1>
                <div>Neither is text in another element.</div>
                """;

        assertThat(HTMLParser.getPreviewString(html))
                .isEqualTo("No preview");
    }

    @Test
    void returnsFallbackWhenContentHasOnlyInvalidElements() {
        String html = """
                <h1>A title is not a preview</h1>
                <p>
                    <img src="large-image.jpg" alt="image alternative text">
                    <picture>
                        <source srcset="large-image.webp" type="image/webp">
                        <img src="large-image.png" alt="picture alternative text">
                    </picture>
                </p>
                """;

        assertThat(HTMLParser.getPreviewString(html))
                .isEqualTo("No preview");
    }

    @Test
    void returnsTextFromFirstPlainParagraph() {
        String html = """
                <p>This is a simple preview paragraph.</p>
                <p>This second paragraph must not appear.</p>
                """;

        assertThat(HTMLParser.getPreviewString(html))
                .isEqualTo("This is a simple preview paragraph.");
    }

    @Test
    void keepsVisibleWordsButRemovesMarkupAndMedia() {
        String html = """
                <p class="lead" style="color: red">
                    Before
                    <strong>bold</strong>
                    <em>italic</em>
                    <a href="/article">linked text</a>
                    <span style="font-size: 2rem">nested <code>code</code></span>
                    <img src="large-image.jpg" alt="image alternative text">
                    <picture>
                        <source srcset="large-image.webp" type="image/webp">
                        <img src="large-image.png" alt="picture alternative text">
                    </picture>
                    <video controls poster="poster.jpg">
                        <source src="movie.mp4" type="video/mp4">
                        video fallback text
                    </video>
                    After
                </p>
                <p>This second paragraph must not appear.</p>
                """;

        assertThat(HTMLParser.getPreviewString(html))
                .isEqualTo("Before bold italic linked text nested code After");
    }
}
