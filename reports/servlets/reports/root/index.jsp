<%@ page language="java" import="com.untangle.uvm.*"
%><!DOCTYPE html>

<%
UvmContext uvm = UvmContextFactory.context();
String company = uvm.brandingManager().getCompanyName();
%>
<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title><%=company%> | Reports</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { height: 100%; overflow: hidden; font-family: 'Source Sans Pro', 'Roboto', sans-serif; }
        .reports-header {
            display: flex;
            align-items: center;
            height: 64px;
            background: #16325B;
            padding: 0 16px;
        }
        .reports-header img { height: 24px; }
        .reports-header h2 { color: #CCC; margin-left: 16px; font-size: 18px; font-weight: 400; }
        .reports-iframe {
            width: 100%;
            height: calc(100% - 64px);
            border: none;
        }
    </style>
</head>
<body>
    <div class="reports-header">
        <a href="/reports/"><img src="/images/BrandingLogo.png" alt="<%=company%>"/></a>
        <h2>Reports</h2>
    </div>
    <iframe class="reports-iframe" src="/console/reports?context=REPORTS"></iframe>
</body>
</html>
