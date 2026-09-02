<!DOCTYPE HTML>
<html lang="en">

<head>
  <title>sirnik.Dev</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />

  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bulma@1.0.4/css/bulma.min.css"
    integrity="sha384-DCY3M8xLkMu6c9IKcKbe+jHKMjelnwC0p+SBaxfHxoBYZWdJF2X400UdBCgATtAB" crossorigin="anonymous">
  <link rel="preconnect" href="https://rsms.me/">
  <link rel="stylesheet" href="https://rsms.me/inter/inter.css">
  <link rel="stylesheet" href="/css/site.css">

  <script src="https://unpkg.com/htmx.org@2.0.10"
    integrity="sha384-H5SrcfygHmAuTDZphMHqBJLc3FhssKjG7w/CeCpFReSfwBWDTKpkzPP8c+cLsK+V"
    crossorigin="anonymous"></script>
  <#import "post-preview.ftl" as preview>
</head>

<body>
  <section class="section">
    <div class="container">
      <h1 class="title is-1">
        sirnik.Dev
      </h1>
      <p class="subtitle">
        Ramblings of a very mid web developer
      </p>
      <div id="blogs">
        <!-- posts![] means "if posts is missing, use []"-->
        <#list posts![] as post>
          <#-- Only the final post requests another Slice, and only when one exists. -->
          <@preview.render
            post=post
            shouldLoadMore=(post?is_last && hasNext) />
        </#list>
      </div>
    </div>
  </section>
</body>

</html>
