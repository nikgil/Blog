package dev.sirnik.blog.utils.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;

public class TestBlogPostGenerator {

    private static final String EXTERNAL_LINK = "https://xkcd.com/2928/";

    private final Builder builder;

    private TestBlogPostGenerator(Builder builder) {
        this.builder = builder;
    }

    public List<BlogPost> generatePosts(int seed) {
        String[] loremIpsumStrings = LoremIpsumGenerator.getWords(30)
                .split(" ");

        Random randomiser = new Random(seed);
        List<BlogPost> posts = new ArrayList<>();

        for (int i = 0; i < builder.postsToGenerate; i++) {
            String title = loremIpsumStrings[randomiser
                    .nextInt(loremIpsumStrings.length)]
                    + " "
                    + loremIpsumStrings[randomiser.nextInt(
                            loremIpsumStrings.length)];
            BlogPost b = new BlogPost(title,
                    title.replace(" ", "_") + "_" + seed + "_" + i,
                    LoremIpsumGenerator.getParagraphs(
                            randomiser.nextInt(1, 5),
                            true));
            List<Tag> tags = getRandomTagSubset(randomiser);

            tags.forEach(b::addTag);
            posts.add(b);
            b.setPublished(true);
        }

        for (int index = 0; index < posts.size(); index++) {
            BlogPost post = posts.get(index);
            post.setContent(addGeneratedContent(
                    post, posts, seed, index, randomiser));
        }

        return posts;
    }

    private String addGeneratedContent(BlogPost post, List<BlogPost> allPosts,
            int seed, int postIndex, Random randomiser) {
        int amountOfContent = randomiser.nextInt(0, 30);
        Document document = Jsoup.parseBodyFragment(post.getContent());

        int addedCount = 0;

        while (addedCount < amountOfContent) {
            int percentage = randomiser.nextInt(1, 101);

            if (percentage < 75) {
                addRandomLink(document, post, allPosts, randomiser);
                addedCount++;
            } else if (percentage < 95) {
                if (TestCodeBlockGenerator.addRandomCodeBlocks(document,
                        randomiser)) {
                    addedCount++;
                }
            } else {
                if (builder.imageGenerator != null) {
                    builder.imageGenerator.addRandomImage(
                            document, post, seed, postIndex, randomiser);
                    addedCount++;
                }
            }
        }

        return document.body().html();
    }

    private void addRandomLink(Document document, BlogPost currentPost,
            List<BlogPost> allPosts, Random randomiser) {
        List<Element> paragraphs = document.body().select("p");
        if (paragraphs.isEmpty()) {
            return;
        }

        Element paragraph = paragraphs
                .get(randomiser.nextInt(paragraphs.size()));
        String[] words = paragraph.text().strip().split("\\s+");
        if (words.length == 0 || words[0].isBlank()) {
            return;
        }

        int wordCount = randomiser.nextInt(1, Math.min(5, words.length) + 1);
        int firstWord = randomiser.nextInt(words.length - wordCount + 1);
        String beforeLink = joinWords(words, 0, firstWord);
        String linkText = joinWords(words, firstWord, firstWord + wordCount);
        String afterLink = joinWords(words, firstWord + wordCount,
                words.length);
        String linkTarget = chooseLinkTarget(
                currentPost, allPosts, randomiser);

        paragraph.empty();
        if (!beforeLink.isEmpty()) {
            paragraph.appendText(beforeLink + " ");
        }
        paragraph.appendElement("a")
                .attr("href", linkTarget)
                .attr("preload", !linkText.equals(EXTERNAL_LINK))
                .text(linkText);

        if (!afterLink.isEmpty()) {
            paragraph.appendText(" " + afterLink);
        }
    }

    private String chooseLinkTarget(BlogPost currentPost,
            List<BlogPost> allPosts, Random randomiser) {
        List<BlogPost> internalTargets = allPosts.stream()
                .filter(post -> post != currentPost)
                .toList();

        if (internalTargets.isEmpty() || randomiser.nextBoolean()) {
            return EXTERNAL_LINK;
        }

        BlogPost target = internalTargets.get(
                randomiser.nextInt(internalTargets.size()));
        return "/posts/" + target.getSlug();
    }

    private String joinWords(String[] words, int start, int end) {
        return String.join(" ", List.of(words).subList(start, end));
    }

    private List<Tag> getRandomTagSubset(Random randomiser) {
        int amount = randomiser.nextInt(builder.minTagsPerPost,
                builder.maxTagsPerPost + 1);
        Set<Tag> output = new HashSet<>();

        while (output.size() < amount) {
            output.add(builder.tagsToUse
                    .get(randomiser.nextInt(builder.tagsToUse.size())));
        }

        return new ArrayList<>(output);
    }

    public static class Builder {
        private List<Tag> tagsToUse;

        private int minTagsPerPost;
        private int maxTagsPerPost;

        private int postsToGenerate;
        private TestImageGenerator imageGenerator;

        public Builder() {
            tagsToUse = Collections.emptyList();
            minTagsPerPost = 0;
            maxTagsPerPost = 0;
            postsToGenerate = 0;
        }

        public Builder setMinTagsPerPost(int amount) {
            minTagsPerPost = Math.clamp(amount, 0, maxTagsPerPost);
            return this;
        }

        public Builder setMaxTagsPerPost(int amount) {
            maxTagsPerPost = Math.max(minTagsPerPost, amount);
            return this;
        }

        public Builder setTagsToUse(List<Tag> tags) {
            tagsToUse = new ArrayList<>(tags);
            return this;
        }

        public Builder setPostsToGenerate(int amount) {
            postsToGenerate = Math.max(1, amount);
            return this;
        }

        public Builder setImageGenerator(TestImageGenerator generator) {
            imageGenerator = generator;
            return this;
        }

        public TestBlogPostGenerator build() {
            return new TestBlogPostGenerator(this);
        }
    }
}
