<!DOCTYPE HTML>
<html lang="en">

<head>
  <#import "partials/head.ftl" as head>
    <@head.render title="Register | sirnik.Dev" stylesheet="login" />
    <#import "partials/header.ftl" as header>
      <#import "partials/csrf.ftl" as csrf>
        <#import "/spring.ftl" as spring>
</head>

<#macro field path name label type autocomplete hint="" refill=true attrs="">
  <@spring.bind path />
  <#local errors=spring.status.errorMessages>
    <#local describedBy=[]>
      <#if hint?has_content>
        <#local describedBy=describedBy + ["register-${name}-hint"]>
      </#if>
      <#if errors?has_content>
        <#local describedBy=describedBy + ["register-${name}-error"]>
      </#if>
      <div class="field">
        <label class="label" for="register-${name}">${label}</label>
        <div class="control">
          <input class="input<#if errors?has_content> is-danger</#if>" id="register-${name}" name="${name}"
            type="${type}" autocomplete="${autocomplete}" required ${attrs} <#if
            refill>value="${(spring.status.value!"")?html}"</#if>
          <#if errors?has_content>aria-invalid="true"</#if>
          <#if describedBy?has_content>aria-describedby="${describedBy?join(" ")}"</#if>>
        </div>
        <#if hint?has_content>
          <p class="help" id="register-${name}-hint">${hint}</p>
        </#if>
        <#if errors?has_content>
          <div class="help is-danger" id="register-${name}-error" role="alert">
            <#list errors as message>
              <p>${message?html}</p>
            </#list>
          </div>
        </#if>
      </div>
</#macro>

<body>
  <section class="section login-page">
    <div class="container">
      <@header.render />
      <main class="login-page__main">
        <div class="login-page__card box">
          <#if registered!false>
            <h2 class="title is-3">Request received</h2>
            <p class="content">
              Your registration is waiting for approval. Once it has been approved you will get an email saying you
              can
              log in.
            </p>
            <a class="button is-link is-fullwidth" href="/">Back to all posts</a>
            <#elseif !(registrationEnabled!false)>
              <h2 class="title is-3">Registration is closed</h2>
              <p class="content">New accounts are not being accepted right now.</p>
              <p class="has-text-centered">Already have an account? <a href="/login">Log in</a></p>
              <#else>
                <h2 class="title is-3">Register</h2>

                <form hx-post="/register" hx-target=".login-page" hw-select=".login-page">
                  <@csrf.field />

                  <#-- Global (non-field) errors, e.g. a rate limit; field errors render under their own input. -->
                    <@spring.bind "registrationForm" />
                    <#if spring.status.errorMessages?has_content>
                      <div class="notification is-danger is-light" role="alert">
                        <#list spring.status.errorMessages as message>
                          <p>${message?html}</p>
                        </#list>
                      </div>
                    </#if>

                    <@field path="registrationForm.name" name="name" label="Name" type="text" autocomplete="name"
                      attrs='maxlength="100"' />
                    <@field path="registrationForm.email" name="email" label="Email" type="email" autocomplete="email"
                      hint="You will get an email here once your account is approved." attrs='maxlength="254"' />
                    <#-- bcrypt only uses the first 72 bytes of a password, so the upper bound matters. These limits are
                      UI hints; the controller's validation is the authority and must use the same numbers. -->
                      <@field path="registrationForm.password" name="password" label="Password" type="password"
                        autocomplete="new-password" refill=false hint="8 to 72 characters."
                        attrs='minlength="8" maxlength="72"' />

                      <div class="field">
                        <div class="control">
                          <button class="button is-link is-fullwidth" type="submit">Register</button>
                        </div>
                      </div>
                </form>

                <p class="has-text-centered mt-4">Already have an account? <a href="/login">Log in</a></p>
          </#if>
        </div>
      </main>
    </div>
  </section>
</body>

</html>
