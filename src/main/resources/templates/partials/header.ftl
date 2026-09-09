<#macro render hrefTarget="/">
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
            <form class="site-search posts-filter" hx-get="/" role="search" hx-target="#blogs" hx-select="#blogs"
                hx-include=".posts-filter" hx-swap="innerHTML"
                hx-trigger="input changed delay:750ms, keyup[key=='Enter']" hx-replace-url="true">
                <label class="is-sr-only" for="site-search-query">Search the blog</label>
                <input class="site-search__input" id="site-search-query" name="query" type="search" placeholder="Search"
                    autocomplete="off">
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
