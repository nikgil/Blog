<!DOCTYPE HTML>
<html lang="en">

<head>
  <#import "../partials/head.ftl" as head>
  <@head.render title="Page not found | sirnik.Dev" stylesheet="error" />
  <#import "../partials/header.ftl" as header>
  <#import "../partials/error-card.ftl" as errorCard>
</head>

<body>
  <section class="section error-page">
    <div class="container">
      <@header.render />
      <@errorCard.render
        code=404
        title="This page wandered off."
        description="The address may be wrong, or the page may have moved somewhere new." />
    </div>
  </section>
</body>

</html>
