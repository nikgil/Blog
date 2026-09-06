package dev.sirnik.blog.utils.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
        List<Tag> output = new ArrayList<>();
        List<Tag> newTags = new ArrayList<>();

        if (size < 0 || size > orderedTagList.size()) {
            return Collections.emptyList();
        }

        // Shuffle once and take unique words. Repeated random picks could build
        // two unsaved Tag objects with the same unique slug.
        Collections.shuffle(orderedTagList, randomizer);
        for (String word : orderedTagList.subList(0, size)) {
            Optional<Tag> existingTag = repository.findBySlug(word);

            if (existingTag.isEmpty()) {
                Tag newTag = new Tag(word, word);
                newTags.add(newTag);
                output.add(newTag);
            } else {
                output.add(existingTag.get());
            }
        }

        repository.saveAll(newTags);
        return output;
    }
}
