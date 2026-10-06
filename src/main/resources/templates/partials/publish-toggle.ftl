<span id="publish-toggle-span">
  <#import "csrf.ftl" as csrf>
    <#-- Admin-only publish toggle. Included by post.ftl, and renderable on its own as a fragment view
      ("partials/publish-toggle") needing only "post" , "loggedIn" and the exposed "_csrf" . The form is the single root
      element and carries a stable id, so a response can replace it in place. It is a plain form POST today, so it works
      without JavaScript; "published" carries the state to switch to. -->
      <#if loggedIn!false>
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
      </#if>
</span>
