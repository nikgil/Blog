<!DOCTYPE HTML>
<html lang="en">

<head>
    <#import "partials/head.ftl" as head>
    <@head.render title=(post.title + " | sirnik.Dev") />
    <#import "partials/header.ftl" as header>
</head>

<body hx-ext="preload">
    <section class="section">
        <div class="container post-page">
            <@header.render />

            <main>
                <article class="post">
                    <header class="post__header">
                        <h2 class="title is-2 mb-4">${post.title}</h2>

                        <div class="post__dates" aria-label="Post dates">
                            <span>
                                Created
                                <time datetime="${post.createdAt}">
                                    ${postDateFormatter.format(post.createdAt)}
                                </time>
                            </span>
                            <#if post.updatedAt?string != post.createdAt?string>
                                <span aria-hidden="true">·</span>
                                <span>
                                    Updated
                                    <time datetime="${post.updatedAt}">
                                        ${postDateFormatter.format(post.updatedAt)}
                                    </time>
                                </span>
                            </#if>
                        </div>

                        <#if post.tags?has_content>
                            <ul class="post__tags tags" aria-label="Post tags">
                                <#list post.tags?sort_by("name") as tag>
                                    <li class="tag">${tag.name}</li>
                                </#list>
                            </ul>
                        </#if>
                    </header>

                    <div class="post__body content">
                        <#-- Post content is trusted author HTML stored by the application. -->
                        ${post.content}
                    </div>
                </article>

                <#if olderPost?? || newerPost??>
                    <nav class="post-navigation" aria-label="Adjacent posts">
                        <#if olderPost??>
                            <a class="post-navigation__link post-navigation__link--older"
                                href="/posts/${olderPost.slug}"
                                preload="mouseover">
                                <span class="post-navigation__label">← Previous post</span>
                                <span class="post-navigation__title">${olderPost.title}</span>
                            </a>
                        </#if>

                        <#if newerPost??>
                            <a class="post-navigation__link post-navigation__link--newer"
                                href="/posts/${newerPost.slug}"
                                preload="mouseover">
                                <span class="post-navigation__label">Next post →</span>
                                <span class="post-navigation__title">${newerPost.title}</span>
                            </a>
                        </#if>
                    </nav>
                </#if>
            </main>
        </div>
    </section>
</body>

</html>
