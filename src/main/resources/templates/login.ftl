<!DOCTYPE HTML>
<html lang="en">

<head>
  <#import "partials/head.ftl" as head>
    <@head.render title="Admin login | sirnik.Dev" stylesheet="login" />
    <#import "partials/header.ftl" as header>
    <#import "partials/csrf.ftl" as csrf>
</head>

<body>
    <section class="section login-page">
      <div class="container">
        <@header.render />
        <main class="login-page__main">
          <div class="login-page__card box">
            <#-- Model flags set by the login controller: "loggedIn" (a user is authenticated), "success" (they just
              logged in), and "error" / "logout" from Spring Security's ?error and ?logout redirects. -->
              <#assign isLoggedIn=loggedIn!false>
                <#assign hasError=error!false>

                  <#if isLoggedIn>
                    <h2 class="title is-3">Logged In</h2>
                    <#-- "previousLogin" is the time of the login before this one, already formatted by the controller;
                      absent on the very first login. -->
                      <h3 class="subtitle is-4" id="previous-login">
                        <#if (previousLogin!"")?has_content>Last login: ${previousLogin}<#else>This is your first login
                        </#if>
                      </h3>

                      <form action="/logout" method="post">
                        <@csrf.field />
                        <div class="field">
                          <div class="control">
                            <button class="button is-link is-fullwidth" type="submit">Log out</button>
                          </div>
                        </div>
                      </form>
                      <#else>

                        <form action="/login" method="post">
                          <@csrf.field />

                          <#-- Bulma validation pattern: is-danger on the inputs plus a help line. Spring Security does
                            not say which field was wrong, so both are marked. -->
                            <div class="field">
                              <label class="label is-sr-only" for="login-username">Username</label>
                              <div class="control">
                                <input class="input<#if hasError> is-danger</#if>" id="login-username" name="username"
                                  type="text" placeholder="Username" autocomplete="username" required autofocus <#if
                                  hasError>aria-invalid="true" aria-describedby="login-error"
                  </#if>>
          </div>
      </div>

      <div class="field">
        <label class="label is-sr-only" for="login-password">Password</label>
        <div class="control">
          <input class="input<#if hasError> is-danger</#if>" id="login-password" name="password" type="password"
            placeholder="Password" autocomplete="current-password" required <#if hasError>aria-invalid="true"
          aria-describedby="login-error"</#if>>
        </div>
        <#if hasError>
          <p class="help is-danger" id="login-error" role="alert">Invalid username or password.</p>
        </#if>
      </div>

      <div class="field">
        <div class="control">
          <button class="button is-link is-fullwidth" type="submit">Log in</button>
        </div>
      </div>
      </form>

      <#-- "registrationEnabled" is the .env flag; until a controller puts it in the model the link stays hidden. -->
      <#if registrationEnabled!false>
        <p class="has-text-centered mt-4">Need an account? <a href="/register">Register</a></p>
      </#if>
      </#if>
      </div>
      </main>
      </div>
    </section>
    <#-- Status toasts sit outside the card, fixed to the bottom-left, and fade out via CSS (see login.css). The
      invalid-login error stays inline under the password field. -->
      <#if isLoggedIn && (success!false)>
        <div class="toast-region">
          <div class="notification is-success is-light toast" role="status">You are logged in.</div>
        </div>
        <#elseif !isLoggedIn && (logout!false)>
          <div class="toast-region">
            <div class="notification is-info is-light toast" role="status">You have been logged out.</div>
          </div>
      </#if>
  </body>

</html>
