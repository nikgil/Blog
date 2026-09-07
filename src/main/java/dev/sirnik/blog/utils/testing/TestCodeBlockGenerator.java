package dev.sirnik.blog.utils.testing;

import java.util.Random;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import dev.sirnik.blog.utils.HTMLParser;

public class TestCodeBlockGenerator {
    private static final String[] CODE_SAMPLES = {
            """
                    record PostLink(String title, String slug) {
                    }
                    """,
            """
                    @GetMapping("/posts/{slug}")
                    String showPost(@PathVariable String slug, Model model) {
                        model.addAttribute("slug", slug);
                        return "post";
                    }
                    """,
            """
                    List<String> slugs = posts.stream()
                            .map(BlogPost::getSlug)
                            .toList();
                    """
    };

    private TestCodeBlockGenerator() {
    }

    public static boolean addRandomCodeBlocks(Document document,
            Random randomiser) {
        Element pre = new Element("pre");

        pre.appendElement("code")
                .addClass("language-java")
                .attr("data-lang", "java")
                .text(generateCodeBlock(randomiser));

        return HTMLParser.insertAtRandomPosition(document.body(), pre,
                randomiser,
                true);
    }

    private static String generateCodeBlock(Random randomiser) {
        int amount = randomiser.nextInt(1, 20);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < amount; i++) {
            sb.append(CODE_SAMPLES[randomiser.nextInt(CODE_SAMPLES.length)]
                    .strip()).append("\n");
        }

        return sb.toString();
    }
}
