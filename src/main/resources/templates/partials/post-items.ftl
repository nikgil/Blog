<#import "post-preview.ftl" as preview>
<#list posts![] as post>
  <@preview.render post=post shouldLoadMore=(post?is_last && hasNext) filters=filters />
</#list>
