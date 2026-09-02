package dev.sirnik.blog.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.repositories.TagRepository;

public class TestTagGenerator {

    private TestTagGenerator() {
    }

    public static List<Tag> generateTags(TagRepository repository, int size,
            int seed) {
        Random randomizer = new Random(seed);

        List<String> orderedTagList = new ArrayList<>(
                LoremIpsumGenerator.UNIQUE_WORDS);
        Set<Tag> output = new HashSet<>();
        List<Tag> newTags = new ArrayList<>();

        if (size < 0 || size > orderedTagList.size()) {
            return Collections.emptyList();
        }

        while (output.size() < size) {
            String word = orderedTagList
                    .get(randomizer.nextInt(orderedTagList.size()));
            Optional<Tag> existingTag = repository.findBySlug(word);

            if (!existingTag.isPresent()) {
                Tag newTag = new Tag(word, word);
                newTags.add(newTag);
                output.add(newTag);
            } else {
                output.add(existingTag.get());
            }
        }

        repository.saveAll(newTags);
        return new ArrayList<>(output);
    }
}
