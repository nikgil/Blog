<!DOCTYPE HTML>
<html lang="en">

<head>
  <#import "partials/head.ftl" as head>
    <@head.render title="sirnik.Dev" stylesheet="index" />
    <#import "partials/post-preview.ftl" as preview>
      <#import "partials/header.ftl" as header>
        <#assign currentQuery=(filters.query)!"">
          <#assign currentTag=(filters.tag)!"">
            <#assign currentYear=(filters.year)!"">
              <#assign currentMonth=(filters.month)!"">
</head>

<body>
  <section class="section">
    <div class="container">
      <@header.render hrefTarget="?" filters=filters />
      <div id="archive-layout" class="archive-layout">
        <aside class="archive-sidebar">
          <#include "partials/archive-navigation.ftl">

          <form class="tag-filter" action="/tags" method="get" aria-labelledby="tag-filter-title" hx-get="/tags"
            hx-trigger="load, submit, input changed delay:500ms" hx-target="#tag-filter-results"
            hx-swap="innerHTML">
            <h2 id="tag-filter-title" class="tag-filter__title">Tags</h2>
            <#if currentQuery?has_content>
              <input type="hidden" name="query" value="${currentQuery?html}">
            </#if>
            <#if currentTag?has_content>
              <input type="hidden" name="tag" value="${currentTag?html}">
            </#if>
            <#if currentYear?has_content>
              <input type="hidden" name="year" value="${currentYear?c}">
            </#if>
            <#if currentMonth?has_content>
              <input type="hidden" name="month" value="${currentMonth?c}">
            </#if>
            <div class="field mb-0">
              <label class="is-sr-only" for="tag-filter-query">Search tags</label>
              <div class="control">
                <input id="tag-filter-query" class="input is-small tag-filter__input" type="search" name="tagQuery"
                  value="${(tagQuery!"")?html}" placeholder="Search tags" autocomplete="off"
                  aria-controls="tag-list">
              </div>
            </div>
            <div id="tag-filter-results" class="tag-filter__results" aria-live="polite">
              <#assign tagsLoading=true>
                <#include "partials/tag-list.ftl">
            </div>
          </form>
        </aside>

        <main id="blogs">
          <!-- posts![] means "if posts is missing, use []"-->
          <#list posts![] as post>
            <#-- Only the final post requests another Slice, and only when one exists. -->
              <@preview.render post=post shouldLoadMore=(post?is_last && hasNext) filters=filters />
          </#list>
          <#if !(posts![])?has_content>
            <section class="search-empty" role="status" aria-live="polite">
              <svg class="search-empty__icon" aria-hidden="true" viewBox="0 0 24 24" width="40" height="40">
                <circle cx="11" cy="11" r="7"></circle>
                <path d="m16 16 5 5"></path>
              </svg>
              <h2 class="search-empty__title">No results found</h2>
              <p class="search-empty__message">Try a different search term.</p>
            </section>
          </#if>
        </main>
      </div>
    </div>
  </section>
</body>

</html>
