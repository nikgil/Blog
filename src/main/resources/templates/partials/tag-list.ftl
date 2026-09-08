<#assign activeTagSlug=selectedTagSlug!"">
  <ul id="tag-list" class="tag-filter__list" aria-label="Available tags">
    <#if tagsLoading!false>
      <li class="tag-filter__status">Loading tags…</li>
      <#elseif tags?has_content>
        <#list tags as tag>
          <#if tag_index lt 10>
            <li class="tag-filter__item<#if tag.slug == activeTagSlug> tag-selected__item</#if>">
              <a class="tag tag-filter__link" href="/test-home?tag=${tag.slug?url('UTF-8')}"
                <#if tag.slug == activeTagSlug>aria-current="true"</#if>>
                <span aria-hidden="true">#</span><span>${tag.name}</span>
              </a>
            </li>
          </#if>
        </#list>
        <#else>
          <li class="tag-filter__status">No matching tags.</li>
    </#if>
  </ul>

  <#assign currentTagPage=tagPage!0>
    <nav class="tag-filter__pagination" aria-label="Tag pages">
      <#if hasPreviousTagPage!false>
        <a id="tag-page-previous" class="button is-small tag-filter__page-link"
          href="/tags?page=${currentTagPage - 1}<#if query?has_content>&amp;query=${query?url('UTF-8')}</#if>"
          hx-get="/tags?page=${currentTagPage - 1}<#if query?has_content>&amp;query=${query?url('UTF-8')}</#if>"
          hx-target="#tag-filter-results" hx-swap="innerHTML" aria-label="Previous 10 tags" rel="prev">
          <span aria-hidden="true">←</span>
        </a>
        <#else>
          <button id="tag-page-previous" class="button is-small tag-filter__page-link" type="button"
            aria-label="Previous 10 tags" disabled>
            <span aria-hidden="true">←</span>
          </button>
      </#if>

      <span class="tag-filter__page-number" aria-label="Tag page ${currentTagPage + 1}">
        ${currentTagPage + 1}
      </span>

      <#if hasNextTagPage!false>
        <a id="tag-page-next" class="button is-small tag-filter__page-link"
          href="/tags?page=${currentTagPage + 1}<#if query?has_content>&amp;query=${query?url('UTF-8')}</#if>"
          hx-get="/tags?page=${currentTagPage + 1}<#if query?has_content>&amp;query=${query?url('UTF-8')}</#if>"
          hx-target="#tag-filter-results" hx-swap="innerHTML" aria-label="Next 10 tags" rel="next">
          <span aria-hidden="true">→</span>
        </a>
        <#else>
          <button id="tag-page-next" class="button is-small tag-filter__page-link" type="button"
            aria-label="Next 10 tags" disabled>
            <span aria-hidden="true">→</span>
          </button>
      </#if>
    </nav>
