<%@page language="java" import="com.untangle.uvm.*"%>
<%--
 * $Id$
--%>

<%
UvmContext uvm = UvmContextFactory.context();
String company = uvm.brandingManager().getCompanyName();
String companyUrl = uvm.brandingManager().getCompanyUrl();
%>
<!DOCTYPE html>

<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title><%=company%> | Try Later</title>
<style type="text/css">
    @import "/skins/default/css/user.css";
</style>
</head>
<body class="quarantine">
<div id="content" class="service-error-height">
    <div id="header"><a href="<%=companyUrl%>"><img src="/images/BrandingLogo.png" border="0" alt="<%=company%> logo"/></a><div class="title">Quarantine Service Error</div></div>
    <div id="main">
        <div style="padding: 7em 0 0 10px;  ">
            The <%=company%> Server has encountered an error.
            Please try later.
            Thanks and sorry.
        </div>
    </div>
</div>
</body>
</html>
