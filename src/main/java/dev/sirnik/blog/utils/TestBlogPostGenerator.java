package dev.sirnik.blog.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;

public class TestBlogPostGenerator {

    private Builder builder;

    private TestBlogPostGenerator(Builder builder) {
        this.builder = builder;
    }

    public List<BlogPost> generatePosts(int seed) {
        String[] loremIpsumStrings = LoremIpsumGenerator.getWords(30)
                .split(" ");

        Random randomiser = new Random(seed);
        List<BlogPost> lst = new ArrayList<>();

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
            lst.add(b);
            b.setPublished(true);
        }

        return lst;
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

        public TestBlogPostGenerator build() {
            return new TestBlogPostGenerator(this);
        }
    }
}
