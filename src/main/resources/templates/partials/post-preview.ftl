<#macro render post shouldLoadMore>
    <article class="post-preview" hx-ext="preload" <#if shouldLoadMore>
        hx-trigger="revealed"
        hx-swap="afterend"
        hx-get="/test-home?page=${nextPage}&amp;throttle=${throttle?c}<#if selectedTagSlug?has_content>&amp;tag=${selectedTagSlug?url('UTF-8')}</#if>"
        hx-select="#blogs > .post-preview"
        </#if>
        >
        <a class="post-preview__link" href="/posts/${post.slug}" preload-images="true" preload>
            <header class="mb-4">
                <h2 class="title is-4 mb-3">${post.title}</h2>
                <time class="post-preview__date" datetime="${post.createdAt}">
                    ${postDateFormatter.format(post.createdAt)}
                </time>
                <#if post.orderedTags?has_content>
                    <section class="post-preview__tag-scroll" aria-label="Tags for ${post.title}">
                        <ul class="post-preview__tags tags">
                            <#list post.orderedTags as tag>
                                <li class="tag">${tag.name}</li>
                            </#list>
                        </ul>
                    </section>
                </#if>
            </header>

            <div class="post-preview__body content">
                ${post.preview?html}
            </div>
        </a>
        <#if shouldLoadMore>
            <output class="post-preview__loading htmx-indicator" aria-live="polite">
                <span class="post-preview__spinner" aria-hidden="true"></span>
                <span class="is-sr-only">Loading more posts…</span>
            </output>
        </#if>
    </article>
</#macro>
