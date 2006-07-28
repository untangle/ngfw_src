<%@ page language="java" import="com.metavize.mvvm.portal.PortalLogin"%>

<%
String sp = (String)request.getContextPath() + "/secure";
String target = request.getParameter("target");
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<title>Metavize Secure Portal</title>
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

<script type="text/javascript" src="/AjaxTk/BundledAjx.js"></script>

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
DBG = new AjxDebug(AjxDebug.NONE, null, false);

var shell = new DwtShell("MainShell", false);
new Browser(shell<%=null == target ? "" : (", '" + target + "'")%>);
}
AjxCore.addOnloadListener(launch);
</script>

</body>

</html>
