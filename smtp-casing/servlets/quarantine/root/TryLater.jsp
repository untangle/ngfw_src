<%@page language="java" import="com.untangle.uvm.*"%>
<!DOCTYPE html>
<%
UvmContext uvm = UvmContextFactory.context();
String companyName = uvm.brandingManager().getCompanyName();
%>
<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title><%=companyName%> | Try Later</title>
    <link href="/ext6.2/fonts/font-awesome/css/font-awesome.min.css" rel="stylesheet" />
    <link href="/ext6.2/fonts/source-sans-pro/css/fonts.css" rel="stylesheet" />
 </head>
<body style="margin: 0; padding: 0; font-family: 'Source Sans Pro', sans-serif;">
    <div style="background: #1b1e26; height: 52px; color: #FFF;">
        <img src="/images/BrandingLogo.png" style="height: 40px; margin: 6px; vertical-align: middle;"/>
        <span style="font-size: 16px; color: #CCC;">Quarantine Service Error</span>
    </div>


    <h3 style="font-weight: 400; text-align: center; margin-top: 100px;">
        <i class="fa fa-exclamation-triangle fa-3x" style="color: orange;"></i><br/><br/>
        The ${companyName} Server has encountered an error!<br/>Please try later. Thanks and sorry.
    </h3>
</body>
</html>
