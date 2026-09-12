<#macro render hrefTarget="/" filters={} partialSearch=false>
    <#local currentQuery=(filters.query)!"">
    <#local currentTag=(filters.tag)!"">
    <#local currentYear=(filters.year)!"">
    <#local currentMonth=(filters.month)!"">
    <header class="site-header">
        <div class="site-header__identity">
            <a href="${hrefTarget}" id="blog-title">
                <h1 class="title is-1">
                    sirnik.Dev
                </h1>
            </a>
            <p class="subtitle">
                Ramblings of a very mid web developer
            </p>
        </div>

        <nav class="site-nav" aria-label="Primary navigation">
            <form id="post-filters" class="site-search" action="/" method="get" role="search"
                <#if partialSearch>
                hx-get="/" hx-target="#blog-content" hx-swap="outerHTML"
                hx-trigger="submit, input changed delay:750ms" hx-replace-url="true"
                hx-include="#tag-filter-query, #tag-page-state" hx-sync="this:replace"
                </#if>>
                <label class="is-sr-only" for="site-search-query">Search the blog</label>
                <input class="site-search__input" id="site-search-query" name="query" type="search" placeholder="Search"
                    autocomplete="off" value="${currentQuery?html}">
                <#if currentTag?has_content>
                    <input type="hidden" name="tag" value="${currentTag?html}">
                </#if>
                <#if currentYear?has_content>
                    <input type="hidden" name="year" value="${currentYear?c}">
                </#if>
                <#if currentMonth?has_content>
                    <input type="hidden" name="month" value="${currentMonth?c}">
                </#if>
                <button class="site-search__button" type="submit" aria-label="Search">
                    <span class="site-search__indicator htmx-indicator" aria-hidden="true"></span>
                    <svg class="site-search__icon" aria-hidden="true" viewBox="0 0 24 24" width="20" height="20">
                        <circle cx="11" cy="11" r="7"></circle>
                        <path d="m16 16 5 5"></path>
                    </svg>
                </button>
            </form>

            <a class="site-nav__link" href="mailto:blog@sirnik.dev">Contact</a>
            <a class="site-nav__link" href="/ai-usage">AI Usage</a>
            <a class="site-nav__link" href="/about">About Me</a>
        </nav>
    </header>
</#macro>
