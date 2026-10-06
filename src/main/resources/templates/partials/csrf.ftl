<#-- Spring Security rejects every POST without this token. _csrf only exists in FreeMarker when
  spring.freemarker.expose-request-attributes=true. Put <@csrf.field /> inside every method="post" form. -->
<#macro field>
  <#if _csrf??>
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
  </#if>
</#macro>
