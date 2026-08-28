package dev.sirnik.blog.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;

public class TestModelGenerator {

    private static final int VALUES_PER_PAGE = 10;
    private static final int MAX_TAGS_PER_POST = 5;

    // Static class, no construction
    private TestModelGenerator() {}

    public static List<BlogPost> generatePosts(int page) {
        String[] loremIpsumStrings = LoremIpsumGenerator.getWords(30).split(" ");

        Random seed = new Random(page);
        List<BlogPost> lst = new ArrayList<>();

        for(int i = 0; i < VALUES_PER_PAGE; i++) {
            String title = 
                loremIpsumStrings[seed.nextInt(loremIpsumStrings.length)] 
                + " " 
                + loremIpsumStrings[seed.nextInt(loremIpsumStrings.length)];
            BlogPost b = new BlogPost(title, title.replaceAll(" ", "_"), LoremIpsumGenerator.getParagraphs(seed.nextInt(1, 5), true));
            List<Tag> tags = generateTags(page, seed.nextInt(1, MAX_TAGS_PER_POST));
            
            tags.forEach(t -> b.addTag(t));

            lst.add(b);
        }

        return lst;
    }

    private static List<Tag> generateTags(int page, int amount) {
        String[] loremIpsumStrings = LoremIpsumGenerator.getWords(30).split(" ");

        Random seed = new Random(page);
        List<Tag> lst = new ArrayList<>();
        
        for(int i = 0; i < amount; i++) {
            String val = loremIpsumStrings[seed.nextInt(loremIpsumStrings.length)];

            lst.add(new Tag(val, val));
        }

        return lst;
    }

}
