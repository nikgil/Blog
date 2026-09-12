<#assign activeTagSlug=(filters.tag)!"">
<#assign currentQuery=(filters.query)!"">
<#assign currentYear=(filters.year)!"">
<#assign currentMonth=(filters.month)!"">

<ul id="tag-list" class="tag-filter__list" aria-label="Available tags">
  <#if tagsLoading!false>
    <li class="tag-filter__status">Loading tags…</li>
  <#elseif tags?has_content>
    <#list tags as tag>
      <#if tag_index lt 10>
        <#assign postParameters=[]>
        <#if tag.slug != activeTagSlug>
          <#assign postParameters += ["tag=" + tag.slug?url("UTF-8")]>
        </#if>
        <#if currentYear?has_content>
          <#assign postParameters += ["year=" + currentYear?c]>
        </#if>
        <#if currentMonth?has_content>
          <#assign postParameters += ["month=" + currentMonth?c]>
        </#if>
        <#if currentQuery?has_content>
          <#assign postParameters += ["query=" + currentQuery?url("UTF-8")]>
        </#if>
        <li class="tag-filter__item <#if tag.slug == activeTagSlug>tag-selected__item</#if>">
          <a class="tag tag-filter__link"
            href="/<#if postParameters?has_content>?${postParameters?join('&amp;')}</#if>"
            <#if tag.slug == activeTagSlug>aria-current="true"</#if>>
            <span aria-hidden="true">#</span><span>${tag.name}</span><span>(${tag.postCount})</span>
            <#if tag.slug == activeTagSlug><span>x</span></#if>
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
    <button id="tag-page-previous" class="button is-small tag-filter__page-link" type="submit" name="page"
      value="${currentTagPage - 1}" aria-label="Previous 10 tags">
      <span aria-hidden="true">←</span>
    </button>
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
    <button id="tag-page-next" class="button is-small tag-filter__page-link" type="submit" name="page"
      value="${currentTagPage + 1}" aria-label="Next 10 tags">
      <span aria-hidden="true">→</span>
    </button>
  <#else>
    <button id="tag-page-next" class="button is-small tag-filter__page-link" type="button"
      aria-label="Next 10 tags" disabled>
      <span aria-hidden="true">→</span>
    </button>
  </#if>
</nav>
