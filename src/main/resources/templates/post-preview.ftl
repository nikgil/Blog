<#macro render post isLast>
    <article class="post-preview" <#if isLast>
        hx-trigger="revealed"
        hx-swap="afterend"
        hx-get="/test-home?page=${nextPage}&throttle=${throttle?c}"
        hx-select="#blogs > .post-preview"
        </#if>
        >
        <header class="mb-4">
            <h2 class="title is-4 mb-3">${post.title}</h2>
            <time class="post-preview__date" datetime="${post.createdAt}">
                ${postDateFormatter.format(post.createdAt)}
            </time>
            <#if post.orderedTags?has_content>
                <section class="post-preview__tag-scroll" aria-label="Tags for ${post.title}">
                    <ul class="post-preview__tags tags">
                        <#list post.orderedTags as tag>
                            <li class="tag is-light">${tag.name}</li>
                        </#list>
                    </ul>
                </section>
            </#if>
        </header>

        <div class="post-preview__body content">
            ${post.content}
        </div>

        <#if isLast>
            <output class="post-preview__loading htmx-indicator" aria-live="polite">
                <span class="post-preview__spinner" aria-hidden="true"></span>
                <span class="is-sr-only">Loading more posts…</span>
            </output>
        </#if>
    </article>
</#macro>
