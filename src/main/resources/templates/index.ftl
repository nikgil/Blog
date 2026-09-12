<!DOCTYPE HTML>
<html lang="en">
<head>
  <#import "partials/head.ftl" as head>
  <#import "partials/header.ftl" as header>
  <@head.render title="sirnik.Dev" stylesheet="index" />
</head>
<body>
  <section class="section">
    <div class="container">
      <@header.render hrefTarget="?" filters=filters partialSearch=true />
      <#include "partials/blog-content.ftl">
    </div>
  </section>
</body>
</html>
