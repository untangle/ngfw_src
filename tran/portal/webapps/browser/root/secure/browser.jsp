<%@ page language="java" import="com.metavize.mvvm.portal.PortalLogin"%>

<%
String sp = (String)request.getContextPath() + "/secure";
String target = request.getParameter("target");
PortalLogin pl = (PortalLogin)request.getUserPrincipal();
String principal = pl.getNtlmAuth().toString();
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<title>Metavize Portal</title>
<style type="text/css">
<!--
@import url(/AjaxTk/img/dwtimgs.css);
-->
<!--
@import url(/images/mvimgs.css);
-->
</style>
<link rel="stylesheet" type="text/css" href="/AjaxTk/common.css"/>
<link rel="stylesheet" type="text/css" href="/AjaxTk/dwt.css"/>
<link rel="stylesheet" type="text/css" href="<%=sp%>/browser.css"/>

<script type="text/javascript" src="/AjaxTk/I18nMsg.js"></script>
<script type="text/javascript" src="/AjaxTk/AjxMsg.js"></script>

<jsp:include page="Ajax.inc"/>

<script type="text/javascript" src="<%=sp%>/MvRpc.js"></script>

<script type="text/javascript" src="<%=sp%>/Browser.js"></script>
<script type="text/javascript" src="<%=sp%>/CifsNode.js"></script>
<script type="text/javascript" src="<%=sp%>/DetailPanel.js"></script>
<script type="text/javascript" src="<%=sp%>/DirTree.js"></script>
<script type="text/javascript" src="<%=sp%>/FileUploadDialog.js"></script>
<script type="text/javascript" src="<%=sp%>/FileUploadPanel.js"></script>
<script type="text/javascript" src="<%=sp%>/LoginDialog.js"></script>
<script type="text/javascript" src="<%=sp%>/MkdirDialog.js"></script>
<script type="text/javascript" src="<%=sp%>/RenameDialog.js"></script>

</head>

<body>

<noscript><p><b>Please enable JavaScript to use this application</b></p></noscript>

<script language="JavaScript">
function launch() {
DBG = new AjxDebug(AjxDebug.DBG1, null, false);

var shell = new DwtShell("MainShell", false);
new Browser(shell, "<%=target%>", "<%=principal%>");
}
AjxCore.addOnloadListener(launch);
</script>

</body>

</html>
