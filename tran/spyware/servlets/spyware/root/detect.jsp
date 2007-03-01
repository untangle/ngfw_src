<%@ page language="java" import="com.untangle.mvvm.*, com.untangle.mvvm.tran.*, com.untangle.mvvm.security.*, com.untangle.tran.spyware.*"%>

<HTML>
<HEAD>
<TITLE>403 Forbidden</TITLE>
</HEAD>
<BODY>

<script id='untangleDetect' type='text/javascript'>
var e = document.getElementById("untangleDetect");
if (window == window.top && e.parentNode.tagName == "BODY") {
  window.location.href = '/spyware/blockpage.jsp?<%=request.getQueryString()%>';
}
</script>

</BODY>
</HTML>

