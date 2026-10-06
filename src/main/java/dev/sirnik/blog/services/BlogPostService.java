package dev.sirnik.blog.services;

import java.util.Optional;

import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.repositories.BlogPostPredicates;
import dev.sirnik.blog.repositories.BlogPostRepository;
import dev.sirnik.blog.utils.AuthenticationUtils;

@Service
public class BlogPostService {

    private final BlogPostRepository blogPostRepository;

    public BlogPostService(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    /**
     * Gets the post found by slug. If user has invalid auth then will only get
     * published.
     */
    @Transactional(readOnly = true)
    public Optional<BlogPost> findBySlug(String slug, Authentication auth) {
        PredicateSpecification<BlogPost> predicates = BlogPostPredicates
            .hasSlug(slug);

        if (!AuthenticationUtils.isValidAdmin(auth)) {
            predicates = predicates.and(BlogPostPredicates.isPublished());
        }

        return blogPostRepository.findOne(predicates);
    }

    @Transactional
    public Optional<BlogPost> togglePublished(String slug) {
        Optional<BlogPost> post = blogPostRepository
            .findOne(BlogPostPredicates.hasSlug(slug));

        post.ifPresent(found -> found.setPublished(!found.isPublished()));
        return post;
    }
}
