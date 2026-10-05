<!DOCTYPE HTML>
<html lang="en">

<head>
  <#import "partials/head.ftl" as head>
    <@head.render title="Admin login | sirnik.Dev" stylesheet="login" />
    <#import "partials/header.ftl" as header>
</head>

<body>
  <section class="section login-page">
    <div class="container">
      <@header.render />
      <main class="login-page__main">
        <div class="login-page__card box">
          <#-- "error" and "logout" are model flags the login controller sets from Spring Security's ?error and ?logout
            redirects. -->
            <#assign hasError=error!false>
              <#if logout!false>
                <div class="notification is-info is-light" role="status">
                  You have been logged out.
                </div>
              </#if>

              <form action="/login" method="post">
                <#-- Spring Security rejects the POST without this token; _csrf only exists in FreeMarker when request
                  attributes are exposed. -->
                  <#if _csrf??>
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                  </#if>

                  <#-- Bulma validation pattern: is-danger on the inputs plus a help line. Spring Security does not say
                    which field was wrong, so both are marked. -->
                    <div class="field">
                      <label class="label is-sr-only" for="login-username">Username</label>
                      <div class="control">
                        <input class="input<#if hasError> is-danger</#if>" id="login-username" name="username"
                          type="text" placeholder="Username" autocomplete="username" required autofocus <#if
                          hasError>aria-invalid="true" aria-describedby="login-error"</#if>>
                      </div>
                    </div>

                    <div class="field">
                      <label class="label is-sr-only" for="login-password">Password</label>
                      <div class="control">
                        <input class="input<#if hasError> is-danger</#if>" id="login-password" name="password"
                          type="password" placeholder="Password" autocomplete="current-password" required <#if
                          hasError>aria-invalid="true" aria-describedby="login-error"</#if>>
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
        </div>
      </main>
    </div>
  </section>
</body>

</html>
