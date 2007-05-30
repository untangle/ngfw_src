<%@ page language="java" import="com.untangle.mvvm.*, com.untangle.mvvm.tran.*, com.untangle.mvvm.security.*, com.untangle.tran.spyware.*"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>403 Forbidden</title>
</head>
<body>
<script id="untangleDetect" type="text/javascript">
// <![CDATA[
var e = document.getElementById("untangleDetect");
if (window == window.top && e.parentNode.tagName == "BODY") {
  window.location.href = '/spyware/blockpage.jsp?<%=request.getQueryString()%>';
}
// ]]>
</script>
</body>
</html>
