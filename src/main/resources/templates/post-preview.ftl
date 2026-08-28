<#macro render post isLast>
    <article class="post-preview" 
        <#if isLast>
            hx-trigger="revealed" 
            hx-swap="afterend" 
            hx-get="/test-home?page=${nextPage}" 
            hx-select="#blogs > .post-preview"
        </#if>
    >
        <header class="mb-4">
            <h2 class="title is-4 mb-3">${post.title}</h2>

            <#if post.orderedTags?has_content>
                <div
                    class="post-preview__tag-scroll"
                    role="region"
                    aria-label="Tags for ${post.title}"
                    tabindex="0"
                >
                    <div class="post-preview__tags tags" role="list">
                        <#list post.orderedTags as tag>
                            <span class="tag is-light" role="listitem">${tag.name}</span>
                        </#list>
                    </div>
                </div>
            </#if>
        </header>

        <div class="post-preview__body content">
            ${post.content}
        </div>
    </article>
</#macro>
