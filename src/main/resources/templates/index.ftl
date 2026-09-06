<!DOCTYPE HTML>
<html lang="en">

<head>
  <#import "partials/head.ftl" as head>
    <@head.render title="sirnik.Dev" />
    <#import "partials/post-preview.ftl" as preview>
      <#import "partials/header.ftl" as header>
</head>

<body>
  <section class="section">
    <div class="container">
      <@header.render />
      <div id="blogs">
        <!-- posts![] means "if posts is missing, use []"-->
        <#list posts![] as post>
          <#-- Only the final post requests another Slice, and only when one exists. -->
            <@preview.render post=post shouldLoadMore=(post?is_last && hasNext) />
        </#list>
      </div>
    </div>
  </section>
</body>

</html>
