<#assign currentQuery=(filters.query)!"">
<#assign currentTag=(filters.tag)!"">
<#assign currentYear=(filters.year)!"">
<#assign currentMonth=(filters.month)!"">
<#assign monthNames=[ "January" , "February" , "March" , "April" , "May" , "June" , "July" , "August" , "September"
  , "October" , "November" , "December" ]>

  <nav id="archive-navigation" class="archive-nav" aria-label="Post archive"
    hx-boost="true" hx-target="#blog-content" hx-swap="outerHTML show:none" hx-sync="#post-filters:replace"
    hx-include="#tag-filter-query, #tag-page-state">
    <p class="archive-nav__title">Archive</p>
    <ol id="archive-years" class="archive-nav__years">
      <#list archiveMonths!{} as year, valueList>
        <#assign isCurYear=(currentYear?has_content && year==currentYear) />
        <li class="archive-nav__year">
          <a class="archive-nav__link archive-nav__year-link <#if isCurYear>archive-nav__selected-link</#if>"
            href="/?<#if !isCurYear>year=${year?c}</#if><#if currentTag?has_content>&amp;tag=${currentTag?url('UTF-8')}</#if><#if currentQuery?has_content>&amp;query=${currentQuery?url('UTF-8')}</#if>">
            <span>${year?c}</span>
            <#if isCurYear>
              <span class="archive-nav__clear" aria-hidden="true">×</span>
              <span class="is-sr-only"> — clear year filter</span>
            </#if>
          </a>
          <ol class="archive-nav__months">
            <#list valueList as archiveMonth>
              <#assign isCurMonth=(currentMonth?has_content && archiveMonth.month==currentMonth) />
              <li class="archive-nav__month">
                <a class="archive-nav__link archive-nav__month-link <#if isCurYear && isCurMonth>archive-nav__selected-link</#if>"
                  href="/?year=${year?c}<#if !(isCurYear && isCurMonth)>&amp;month=${archiveMonth.month?c}</#if><#if currentTag?has_content>&amp;tag=${currentTag?url('UTF-8')}</#if><#if currentQuery?has_content>&amp;query=${currentQuery?url('UTF-8')}</#if>">
                  <span>
                    ${monthNames[archiveMonth.month - 1]}
                  </span>
                  <#if isCurYear && isCurMonth>
                    <span class="archive-nav__clear" aria-hidden="true">×</span>
                    <span class="is-sr-only"> — clear month filter</span>
                  </#if>
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
