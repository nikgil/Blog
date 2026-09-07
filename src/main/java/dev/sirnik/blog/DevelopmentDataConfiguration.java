package dev.sirnik.blog;

import java.nio.file.Path;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.models.Tag;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.repositories.TagRepository;
import dev.sirnik.blog.utils.testing.TestBlogPostGenerator;
import dev.sirnik.blog.utils.testing.TestImageGenerator;
import dev.sirnik.blog.utils.testing.TestTagGenerator;

@Configuration
@Profile("!prod & !test")
public class DevelopmentDataConfiguration implements WebMvcConfigurer {

    private static final int TEST_DATA_SEED = 20_260_906;
    private static final Path GENERATED_IMAGE_DIRECTORY = Path
        .of("target", "generated-test-images")
        .toAbsolutePath()
        .normalize();
    private static final String GENERATED_IMAGE_PATH = "/generated-test-images/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = GENERATED_IMAGE_DIRECTORY.toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        registry
            .addResourceHandler(GENERATED_IMAGE_PATH + "**")
            .addResourceLocations(resourceLocation);
    }

    @Bean
    TestImageGenerator testImageGenerator() {
        return new TestImageGenerator(GENERATED_IMAGE_DIRECTORY,
            GENERATED_IMAGE_PATH);
    }

    @Bean
    ApplicationRunner seedBlogPosts(TagRepository tagRepository,
        BlogPostRepository blogPostRepository,
        TestImageGenerator imageGenerator) {
        return arguments -> {
            // DevTools can restart Spring while the in-memory H2
            // database stays
            // alive. Seed once so a restart cannot violate unique
            // slugs.
            if (blogPostRepository.count() > 0) {
                return;
            }

            List<Tag> allTags = TestTagGenerator
                .generateTags(tagRepository, 10, TEST_DATA_SEED);

            TestBlogPostGenerator blogPostGenerator = new TestBlogPostGenerator.Builder()
                .setMaxTagsPerPost(5)
                .setMinTagsPerPost(1)
                .setTagsToUse(allTags)
                .setPostsToGenerate(50)
                .setImageGenerator(imageGenerator)
                .setMinimumTime(ZonedDateTime
                    .now(ZoneOffset.UTC)
                    .minus(Period.ofYears(2))
                    .toInstant())
                .setMaximumTime(Instant.now())
                .build();

            List<BlogPost> samplePosts = blogPostGenerator
                .generatePosts(TEST_DATA_SEED);
            blogPostRepository.saveAll(samplePosts);
        };
    }

}
