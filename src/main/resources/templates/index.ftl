<!DOCTYPE HTML>
<html lang="en">

<head>
  <#import "partials/head.ftl" as head>
    <@head.render title="sirnik.Dev" stylesheet="index" />
    <#import "partials/post-preview.ftl" as preview>
      <#import "partials/header.ftl" as header>
        <#assign monthNames=[ "January" , "February" , "March" , "April" , "May" , "June" , "July" , "August"
          , "September" , "October" , "November" , "December" ]>
</head>

<body>
  <section class="section">
    <div class="container">
      <@header.render hrefTarget="?" />
      <div class="archive-layout">
        <aside class="archive-sidebar">
          <nav id="archive-navigation" class="archive-nav" aria-label="Post archive">
            <p class="archive-nav__title">Archive</p>
            <ol id="archive-years" class="archive-nav__years">
              <#list archiveMonths!{} as year, valueList>
                <li class="archive-nav__year">
                  <a class="archive-nav__link archive-nav__year-link posts-filter" href="?year=${year?c}"
                    hx-include=".posts-filter">
                    ${year?c}
                  </a>
                  <ol class="archive-nav__months">
                    <#list valueList as archiveMonth>
                      <li class="archive-nav__month">
                        <a class="archive-nav__link archive-nav__month-link posts-filter"
                          href="?year=${year?c}&amp;month=${archiveMonth.month?c}" hx-include=".posts-filter">
                          <span>${monthNames[archiveMonth.month - 1]}</span>
                          <span class="archive-nav__count" aria-label="${archiveMonth.postCount?c} posts">
                            ${archiveMonth.postCount?c}
                          </span>
                        </a>
                      </li>
                    </#list>
                  </ol>
                </li>
              </#list>
            </ol>
          </nav>

          <section class="tag-filter" aria-labelledby="tag-filter-title" hx-include="#selected-tag">
            <h2 id="tag-filter-title" class="tag-filter__title">Tags</h2>
            <input id="selected-tag" type="hidden" name="tag" value="${selectedTagSlug!}">
            <div class="field mb-0">
              <label class="is-sr-only" for="tag-filter-query">Search tags</label>
              <div class="control">
                <input id="tag-filter-query" class="input is-small tag-filter__input" type="search" name="query"
                  placeholder="Search tags" autocomplete="off" aria-controls="tag-list"
                  hx-trigger="input changed delay:500ms, keyup[key=='Enter']" hx-target="#tag-filter-results"
                  hx-swap="innerHTML" hx-get="/tags">
              </div>
            </div>
            <div id="tag-filter-results" class="tag-filter__results" aria-live="polite" hx-swap="innerHTML"
              hx-get="/tags" hx-trigger="load">
              <#assign tagsLoading=true>
                <#include "partials/tag-list.ftl">
            </div>
          </section>
        </aside>

        <main id="blogs">
          <!-- posts![] means "if posts is missing, use []"-->
          <#list posts![] as post>
            <#-- Only the final post requests another Slice, and only when one exists. -->
              <@preview.render post=post shouldLoadMore=(post?is_last && hasNext) />
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
