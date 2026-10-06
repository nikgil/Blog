<#import "csrf.ftl" as csrf>
  <#if loggedIn!false>
    <span id="publish-toggle-span">
      <form id="publish-toggle" class="post__admin" method="post" action="/posts/${post.slug?url('UTF-8')}/publish"
        aria-label="Publication status" hx-swap="outerHTML" hx-post="/posts/${post.slug?url('UTF-8')}/publish"
        hx-target="#publish-toggle-span">
        <@csrf.field />
        <input type="hidden" name="published" value="${(!post.published)?c}">
        <span class="tag is-light ${post.published?then('is-success', 'is-warning')}">
          ${post.published?then('Published', 'Unpublished')}
        </span>
        <button class="button is-small is-light ${post.published?then('is-warning', 'is-success')}" type="submit">
          ${post.published?then('Unpublish', 'Publish')}
        </button>
      </form>
    </span>
  </#if>
