<main id="blogs">
  <#include "post-items.ftl">
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
