<!DOCTYPE HTML>
<html lang="en">

<head>
  <#import "partials/head.ftl" as head>
  <@head.render title="Something went wrong | sirnik.Dev" stylesheet="error" />
  <#import "partials/header.ftl" as header>
  <#import "partials/error-card.ftl" as errorCard>
</head>

<body>
  <section class="section error-page">
    <div class="container">
      <@header.render />
      <@errorCard.render
        code=status!500
        title="Something went wrong."
        description="The site hit an unexpected problem. Please try again in a moment." />
    </div>
  </section>
</body>

</html>
