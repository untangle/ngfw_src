<%@page language="java" import="com.untangle.uvm.*"%>
<%@ taglib prefix="uvm" uri="http://java.untangle.com/jsp/uvm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
--%>

<%
UvmContext uvm = UvmContextFactory.context();
String company = uvm.brandingManager().getCompanyName();
String companyUrl = uvm.brandingManager().getCompanyUrl();
request.setAttribute( "i18n_map", uvm.languageManager().getTranslations("directory-connector"));
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | <uvm:i18n>User Notification Login Script</uvm:i18n></title>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
</head>
<body>
<div id="main">
 <!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->

    <div class="un_login_head">
        <a href="<%=companyUrl%>"><img src="/images/BrandingLogo.png" border="0" alt="<%=company%> logo" style="max-width: 150px; max-height: 140px;"/></a> <div><uvm:i18n>User Notification Login Script</uvm:i18n></div>
    </div>

    <hr />

    <center>
    <div style="padding: 10px 0; margin: 0 auto; width: 440px;">

        <span class="un_login_sub_title"><uvm:i18n>Download</uvm:i18n></span>
        <br /><br />
         <a href="/userapi/registration?download=download"><b><uvm:i18n>User Notification Login Script</uvm:i18n></b></a>
         <div  class="un_login_sub_text">
    <p>
         <uvm:i18n>The User Notification Login Script is a small script that runs on client machines that notifies the <%=company%> server when a user logs in. This allows the <%=company%> server to add the username to the appropriate host in the Host Table so the appropriate rules can apply for that user and the events will be recorded as associated with that user.</uvm:i18n>
    </p>
    <p>
         <uvm:i18n>For Active Directory deployments, after downloading the script place it on a share available to all users in the Active Directory Domain. Configure client machines to run the script at login by adding it to the Group Policy under Policy: User Configuration: Windows Settings: Scripts ( Login / Logoff ): Login. The path to the script should be of the form:</uvm:i18n> \\servername\sharename\user_notification.vbs
    </p>
    </div>
    </div>
    </center>

    <hr />

 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>

</body>
</html>
