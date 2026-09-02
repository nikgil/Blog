package dev.sirnik.blog;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;
import dev.sirnik.blog.utils.TestBlogPostGenerator;
import dev.sirnik.blog.utils.TestTagGenerator;

@Configuration
@Profile("!prod & !test")
public class DevelopmentDataConfiguration {

    @Bean
    ApplicationRunner seedBlogPosts(
            TagRepository tagRepository,
            BlogPostRepository blogPostRepository) {
        return arguments -> {
            // DevTools can restart Spring while the in-memory H2 database stays
            // alive. Seed once so a restart cannot violate unique slugs.
            if (blogPostRepository.count() > 0) {
                return;
            }

            List<Tag> allTags = TestTagGenerator.generateTags(tagRepository, 10,
                    -1);

            TestBlogPostGenerator blogPostGenerator =
                    new TestBlogPostGenerator.Builder()
                            .setMaxTagsPerPost(5)
                            .setMinTagsPerPost(1)
                            .setTagsToUse(allTags)
                            .setPostsToGenerate(10)
                            .build();

            List<BlogPost> samplePosts = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                samplePosts.addAll(blogPostGenerator.generatePosts(i));
            }

            samplePosts.forEach(post -> post.setPublished(true));
            blogPostRepository.saveAll(samplePosts);
        };
    }

}
