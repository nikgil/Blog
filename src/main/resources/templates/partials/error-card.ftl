<#macro render code title description>
  <main class="error-page__main">
    <div class="error-page__card box">
      <p class="error-page__code" aria-hidden="true">${code}</p>
      <h2 class="title is-2">${title}</h2>
      <p class="subtitle is-5">${description}</p>
      <a class="button is-link" href="/">
        <span aria-hidden="true">←</span>
        <span>Back to all posts</span>
      </a>
    </div>
  </main>
</#macro>
