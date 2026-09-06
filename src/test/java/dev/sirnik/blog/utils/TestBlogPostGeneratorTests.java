package dev.sirnik.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.utils.testing.LoremIpsumGenerator;
import dev.sirnik.blog.utils.testing.TestBlogPostGenerator;
import dev.sirnik.blog.utils.testing.TestImageGenerator;

class TestBlogPostGeneratorTests {

        private static final String EXTERNAL_LINK = "https://xkcd.com/2928/";

        @TempDir
        private Path temporaryDirectory;

        @Test
        void sameSeedProducesTheSamePostsAndEnrichedContent() {
                List<BlogPost> first = generatorFor(
                                temporaryDirectory.resolve("first"), 5)
                                .generatePosts(1234);
                List<BlogPost> second = generatorFor(
                                temporaryDirectory.resolve("second"), 5)
                                .generatePosts(1234);

                assertThat(first).extracting(BlogPost::getTitle)
                                .containsExactlyElementsOf(
                                                second.stream().map(
                                                                BlogPost::getTitle)
                                                                .toList());
                assertThat(first).extracting(BlogPost::getSlug)
                                .containsExactlyElementsOf(
                                                second.stream().map(
                                                                BlogPost::getSlug)
                                                                .toList());
                assertThat(first).extracting(BlogPost::getContent)
                                .containsExactlyElementsOf(
                                                second.stream().map(
                                                                BlogPost::getContent)
                                                                .toList());
        }

        @Test
        void addsRandomLinksCodeAndResponsiveImages() {
                List<BlogPost> posts = generatorFor(temporaryDirectory, 20)
                                .generatePosts(20260906);
                Set<String> knownSlugs = posts.stream()
                                .map(BlogPost::getSlug)
                                .collect(Collectors.toSet());
                int externalLinkCount = 0;
                int internalLinkCount = 0;
                int codeBlockCount = 0;
                int imageCount = 0;
                int figureCaptionCount = 0;
                int overlayCaptionCount = 0;
                int citationCount = 0;

                for (BlogPost post : posts) {
                        Document document = Jsoup
                                        .parseBodyFragment(post.getContent());

                        assertThat(document.select("pre + pre")).isEmpty();

                        for (Element link : document.select("p a[href]")) {
                                assertThat(link.text()).isNotBlank();
                                assertThat(LoremIpsumGenerator.getParagraphs(4))
                                                .contains(link.text());

                                String target = link.attr("href");
                                if (target.equals(EXTERNAL_LINK)) {
                                        externalLinkCount++;
                                } else {
                                        internalLinkCount++;
                                        assertThat(target).startsWith("/posts/");
                                        String targetSlug = target.substring(
                                                        "/posts/".length());
                                        assertThat(knownSlugs)
                                                        .contains(targetSlug);
                                        assertThat(targetSlug)
                                                        .isNotEqualTo(
                                                                        post.getSlug());
                                }
                        }

                        for (Element code : document.select(
                                        "pre > code.language-java")) {
                                assertThat(code.text()).isNotBlank();
                                codeBlockCount++;
                        }

                        for (Element image : document.select("figure > img")) {
                                assertThat(image.attr("srcset"))
                                                .contains("480w", "960w",
                                                                "1440w");
                                assertThat(image.attr("sizes"))
                                                .contains("max-width: 768px");
                                assertThat(image.attr("loading"))
                                                .isEqualTo("lazy");
                                assertThat(image.attr("decoding"))
                                                .isEqualTo("async");
                                assertThat(image.attr("width"))
                                                .isEqualTo("960");
                                assertThat(image.attr("height"))
                                                .isEqualTo("540");
                                imageCount++;
                        }

                        figureCaptionCount += document
                                        .select("figure > figcaption").size();
                        overlayCaptionCount += document.select(
                                        "figure > figcaption > .generated-test-image__caption")
                                        .size();
                        citationCount += document
                                        .select("figure > figcaption > cite")
                                        .size();
                }

                assertThat(externalLinkCount).isPositive();
                assertThat(internalLinkCount).isPositive();
                assertThat(codeBlockCount).isPositive();
                assertThat(imageCount).isPositive();
                assertThat(figureCaptionCount).isBetween(1, imageCount - 1);
                assertThat(overlayCaptionCount).isBetween(1, imageCount - 1);
                assertThat(citationCount).isBetween(1, imageCount - 1);
        }

        private TestBlogPostGenerator generatorFor(Path imageDirectory,
                        int postCount) {
                TestImageGenerator imageGenerator = new TestImageGenerator(
                                imageDirectory, "/generated-test-images/");
                return new TestBlogPostGenerator.Builder()
                                .setPostsToGenerate(postCount)
                                .setImageGenerator(imageGenerator)
                                .build();
        }
}
