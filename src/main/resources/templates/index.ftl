<!DOCTYPE HTML>
<html lang="en">

<head>
  <title>sirnik.Dev</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />

  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bulma@1.0.4/css/bulma.min.css">
  <link rel="preconnect" href="https://rsms.me/">
  <link rel="stylesheet" href="https://rsms.me/inter/inter.css">
  <link rel="stylesheet" href="/css/site.css">

  <script src="https://unpkg.com/htmx.org@2.0.10"></script>
  <#import "post-preview.ftl" as preview>
</head>

<body>
  <section class="section">
    <div class="container">
      <h1 class="title is-1">
        Hello World
      </h1>
      <p class="subtitle">
        My first website with <strong>Bulma</strong>!
      </p>
      <div id="blogs">
        <!-- posts![] means "if posts is missing, use []"-->
        <#list posts![] as post>
          <@preview.render post=post isLast=post?is_last />
        </#list>
      </div>
    </div>
  </section>
</body>

</html>
