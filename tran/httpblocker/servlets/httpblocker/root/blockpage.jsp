<%@ page language="java" import="com.untangle.mvvm.*, com.untangle.mvvm.tran.*, com.untangle.mvvm.security.*, com.untangle.tran.httpblocker.*"%>

<%
LocalTransformManager tman = MvvmContextFactory.context().transformManager();

String nonce = request.getParameter("nonce");
String tidStr = request.getParameter("tid");
Tid tid = new Tid(Long.parseLong(tidStr));

TransformContext tctx = tman.transformContext(tid);
HttpBlocker tran = (HttpBlocker)tctx.transform();
HttpBlockerBlockDetails bd = tran.getDetails(nonce);

String header = null == bd ? "" : bd.getHeader();
String contact = null == bd ? "your administrator" : bd.getContact();
String host = null == bd ? "" : bd.getFormattedHost();
String url = null == bd ? "" : bd.getFormattedUrl();
String reason = null == bd ? "" : bd.getReason();
%>

<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
  <head>
    <link href="/main.css" rel="stylesheet" type="text/css"/>

<title>Untangle Web Content Control Warning</title>
<script language="JavaScript">
nonce = '<%=nonce%>';
tid = '<%=tidStr%>';
url = '<%=null == bd ? "javascript:history.back()" : bd.getUrl()%>';
</script>

  </head>

  <body style="margin: 20px 20px 20px 20px">
    <table cellspacing="0" cellpadding="0" border="0" align="center" width="100%">
      <tbody>
        <tr>
          <td id="table_main_top_left"><img src="/images/background/spacer.gif" alt=" " height="23" width="23"/></td>
          <td id="table_main_top" width="100%"><img src="/images/background/spacer.gif" alt=" " height="1" width="1"/></td>
          <td id="table_main_top_right"> <img src="/images/background/spacer.gif" alt=" " height="23" width="23"/></td>
        </tr>
        <tr>
          <td id="table_main_left"><img src="/images/background/spacer.gif" alt=" " height="1" width="1"/></td>
          <td id="table_main_center">

<table>
<tbody>
<tr>
  <td colspan=1 width="154px">
<a href="http://www.untangle.com"><img src="/images/Logo150x96.gif" border="0" alt="Untangle logo" width="150" height="96"/></a>
  </td>
  <td style="padding: 0px 0px 0px 10px" class="page_header_title" align="left" valign="middle">
    <%=header%>
  </td>
</tr>
</tbody>
</table>
<table width="100%">
<tbody>
<tr>
<td><hr width="100%" size="1" color="#969696"/></td>
</tr>
<tr>
<td>This web page was blocked because it is considered inappropriate.</td>
</tr>
<tr>
<td><p><b>Host:</b> <%=host%></p></td>
</tr>
<tr>
<td><p><b>URL:</b> <%=url%></p></td>
</tr>
<tr>
<td><p><b>Category:</b> <%=reason%></p></td>
</tr>
<tr>
<td>Please contact <%=contact%>.</td>
</tr>
</tbody>
<tfoot>
<tr>
<td><hr width="100%" size="1" color="#969696"/></td>
</tr>
<tr>
<td><address>Untangle Web Content Control</address></td>
</tr>
</tfoot>
</table>

          </td>
          <td id="table_main_right"><img src="/images/background/spacer.gif" alt=" " height="1" width="1"/></td>
        </tr>
        <tr>
          <td id="table_main_bottom_left"><img src="/images/background/spacer.gif" alt=" " height="23" width="23"/></td>
          <td id="table_main_bottom"><img src="/images/background/spacer.gif" alt=" " height="1" width="1"/></td>
          <td id="table_main_bottom_right"> <img src="/images/background/spacer.gif" alt=" " height="23" width="23"/></td>
        </tr>
      </tbody>
    </table>
  </body>
</html>
