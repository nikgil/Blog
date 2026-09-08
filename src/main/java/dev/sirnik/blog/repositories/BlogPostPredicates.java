package dev.sirnik.blog.repositories;

import java.time.Instant;

import org.springframework.data.jpa.domain.PredicateSpecification;

import dev.sirnik.blog.models.BlogPost;

public final class BlogPostPredicates {

    private BlogPostPredicates() {
    }

    public static PredicateSpecification<BlogPost> isPublished() {
        return (post, builder) -> builder.isTrue(post.get("published"));
    }

    public static PredicateSpecification<BlogPost> createdAtOrAfter(
        Instant startInclusive) {
        return (post, builder) -> builder
            .greaterThanOrEqualTo(post.get("createdAt"), startInclusive);
    }

    public static PredicateSpecification<BlogPost> createdBefore(
        Instant endExclusive) {
        return (post, builder) -> builder
            .lessThan(post.get("createdAt"), endExclusive);
    }

    public static PredicateSpecification<BlogPost> hasTagSlug(String tagSlug) {
        if (tagSlug == null || tagSlug.isBlank()) {
            return PredicateSpecification.unrestricted();
        }

        String normalizedTagSlug = tagSlug.strip();
        return (post, builder) -> builder
            .equal(post.join("tags").get("slug"), normalizedTagSlug);
    }
}
