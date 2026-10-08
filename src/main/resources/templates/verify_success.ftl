<!DOCTYPE HTML>
<html lang="en">

<head>
  <#import "partials/head.ftl" as head>
    <@head.render title="Account approved | sirnik.Dev" stylesheet="login" />
    <#import "partials/header.ftl" as header>
</head>

<#-- Model contract (set by RegistrationController#getConfirmation): username, email. -->
<body>
  <section class="section login-page">
    <div class="container">
      <@header.render />
      <main class="login-page__main">
        <div class="login-page__card box">
          <h2 class="title is-3">Account approved</h2>
          <p class="content">
            The account for <strong>${username?html}</strong> (${email?html}) is now active and can log in.
          </p>
          <a class="button is-link is-fullwidth" href="/">Back to all posts</a>
        </div>
      </main>
    </div>
  </section>
</body>

</html>
