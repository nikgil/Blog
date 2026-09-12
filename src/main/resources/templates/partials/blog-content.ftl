<#import "header.ftl" as header>
<div id="blog-content" class="container">
  <@header.render hrefTarget="?" filters=filters partialSearch=true />
  <div id="archive-layout" class="archive-layout">
    <aside class="archive-sidebar">
      <#include "archive-navigation.ftl">
      <form id="tag-filter" class="tag-filter" action="/tags" method="get" aria-labelledby="tag-filter-title"
        hx-get="/tags" hx-trigger="submit, input changed delay:500ms"
        hx-target="#tag-filter-results" hx-swap="innerHTML" hx-sync="#post-filters:queue last">
        <h2 id="tag-filter-title" class="tag-filter__title">Tags</h2>
        <#if (filters.query!"")?has_content>
          <input type="hidden" name="query" value="${filters.query?html}">
        </#if>
        <#if (filters.tag!"")?has_content>
          <input type="hidden" name="tag" value="${filters.tag?html}">
        </#if>
        <#if (filters.year!"")?has_content>
          <input type="hidden" name="year" value="${filters.year?c}">
        </#if>
        <#if (filters.month!"")?has_content>
          <input type="hidden" name="month" value="${filters.month?c}">
        </#if>
        <div class="field mb-0">
          <label class="is-sr-only" for="tag-filter-query">Search tags</label>
          <div class="control">
            <input id="tag-filter-query" class="input is-small tag-filter__input" type="search" name="tagQuery"
              value="${(tagQuery!"")?html}" placeholder="Search tags" autocomplete="off" aria-controls="tag-list">
          </div>
        </div>
        <div id="tag-filter-results" class="tag-filter__results" aria-live="polite">
          <#include "tag-list.ftl">
        </div>
      </form>
    </aside>
    <#include "post-list.ftl">
  </div>
</div>
