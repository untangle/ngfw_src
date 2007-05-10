<%@ page language="java" import="com.untangle.mvvm.*, com.untangle.mvvm.security.Tid, com.untangle.mvvm.tran.*, com.untangle.mvvm.tapi.*, com.untangle.mvvm.util.SessionUtil, org.apache.log4j.helpers.AbsoluteTimeDateFormat, java.util.Properties, java.net.URL, java.io.PrintWriter, javax.naming.*" %>

<%
MvvmLocalContext mvvm = MvvmContextFactory.context();

BrandingSettings bs = mvvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();

ReportingManager reportingManager = mvvm.reportingManager();

boolean reportingEnabled = reportingManager.isReportingEnabled();
boolean reportsAvailable = reportingManager.isReportsAvailable();
if (!reportsAvailable) {
%>

<html xmlns="http://www.w3.org/1999/xhtml">

  <!-- HEADING -->
  <head>
    <title><%=company%> Reports</title>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
    <style type="text/css">
    <!--
body {
     margin: 15px;
     background: #FFFFFF url(./images/background_body.gif) repeat-x top left;
     text-align: center;
}

table {
    font: normal normal normal 10pt/14pt Arial,Sans-Serif;
    text-align: left;
    color: #606060;
}

input {
    font: normal normal normal 10pt/14pt Arial,Sans-Serif;
    text-align: left;
    color: #606060;
}

form {
    margin: 0px;
}

a:link, a:visited {
    font: normal normal normal 10pt/14pt Arial,Sans-Serif;
    color: #1997FE;
    text-decoration: none;
}

a:hover {
    color: #1997FE;
    text-decoration: underline;
}

.page_header_title {
    font: bold normal normal 25pt Arial,Sans-Serif;
    color: #777777;
}

h1 {
    font: normal normal bold 11pt Arial,Sans-Serif;
    color: #999999;
    letter-spacing: 1px;
    margin: 0px;
    padding: 0px;
}

h2 {
    font: normal normal bold 10pt Arial,Sans-Serif;
    color: #999999;
    letter-spacing: 1px;
    margin: 0px;
    padding: 0px;
}

h3 {
    font: normal normal bold 11pt Arial,Sans-Serif;
    color: #606060;
    letter-spacing: 1px;
    margin: 0px;
    padding: 0px;
}

h4 {
    font: normal normal bold 14pt Arial,Sans-Serif;
    color: #0044D7;
    letter-spacing: 1px;
    margin: 0px;
    padding: 0px;
}

/* the following pertain to the main content (i.e. the main 'metal-brushed') table */
#table_main_center {
    background: url(./images/background_table_main_center.gif) repeat-y;
}

#table_main_top {
    background: url(./images/background_table_main_top.gif) repeat-x;
}

#table_main_top_left {
    background: url(./images/rounded_corner_main_top_left.gif) no-repeat;
}

#table_main_top_right {
    background: url(./images/rounded_corner_main_top_right.gif) no-repeat;
}

#table_main_right {
    background: url(./images/background_table_main_right.gif) repeat-y;
}

#table_main_bottom {
    background: url(./images/background_table_main_bottom.gif) repeat-x;
}

#table_main_bottom_left {
    background: url(./images/rounded_corner_main_bottom_left.gif) no-repeat;
}

#table_main_bottom_right {
    background: url(./images/rounded_corner_main_bottom_right.gif) no-repeat;
}

#table_main_left {
    background: url(./images/background_table_main_left.gif) repeat-y;
}
    -->
   </style>
  </head>


<BODY>


    <center>
      <table border="0" cellpadding="0" cellspacing="0" width="904">

        <!-- TOP THIRD -->
        <tr>
          <td id="table_main_top_left">
            <img src="./images/spacer.gif" alt=" " width="23" height="23"/><br/>
          </td>
          <td width="100%" id="table_main_top">
            <img src="./images/spacer.gif" alt=" " width="1" height="1"/><br/>
          </td>
          <td id="table_main_top_right">
            <img src="./images/spacer.gif" alt=" " width="23" height="23"/><br/>
          </td>
        </tr>
        <!-- END TOP THIRD -->

        <!-- MIDDLE THIRD -->
        <tr>
          <td id="table_main_left">
            <img src="./images/spacer.gif" alt=" " width="1" height="1"/>
          </td>
          <td id="table_main_center">
            <table>
              <tr>
              <td valign="middle">
                <img src="./images/BrandingLogo.gif" alt="<%=company%>" width="150" height="96"/>
              </td>
              <td style="padding: 0px 0px 0px 10px" valign="middle">
                <span class="page_header_title"><%=company%> Reports</span>
              </td>
              </tr>
            </table>
          </td>
          <td id="table_main_right">
            <img src="./images/spacer.gif" alt=" " width="1" height="1"/>
          </td>
        </tr>
        <!-- END MIDDLE THIRD -->

    <tr>
    <td id="table_main_left"></td>
    <td id="table_main_center">
        <center>
        <b><i>
        No reports are available.<br/>
        <br/>

        <% if(!reportingEnabled){ %>
            <%=company%> Reports is not installed into your rack or it is not turned on.<br/>
            Reports are only generated when <%=company%> Reports is installed and turned on.
        <% } else{ %>
            When daily, weekly, and/or monthly Reports are scheduled,<br/>
            please check back the morning after the scheduled day<br/>
            for daily, weekly, and/or monthly Reports.<br/>
            <br/>
            When scheduled, <%=company%> Reports automatically generates<br/>
            the requested Reports during the preceeding night.
        <% } %>

        </i></b>
        </center>
    </td>
    <td id="table_main_right"></td>
    </tr>

        <!-- BOTTOM THIRD -->
        <tr>
          <td id="table_main_bottom_left">
            <img src="./images/spacer.gif" alt=" " width="23" height="23"/><br/>
          </td>
          <td id="table_main_bottom">
            <img src="./images/spacer.gif" alt=" " width="1" height="1"/><br/>
          </td>
          <td id="table_main_bottom_right">
            <img src="./images/spacer.gif" alt=" " width="23" height="23"/><br/>
          </td>
        </tr>
        <!-- END BOTTOM THIRD -->

      </table>
    </center>

</BODY>
</HTML>

<%
    } else {
            // We can redirect them. If it fails (shouldn't with any modern
            // browser), serve them the following backup page.
            response.sendRedirect("./current");

%>

<html><head><title><%=company%> Reports</title>
<STYLE><!---
H1{font-family : sans-serif,Arial,Tahoma;color : white;
  background-color : #0086b2;}
BODY{font-family : sans-serif,Arial,Tahoma;color : black;
  background-color : white;}
B{color : white;background-color : #0086b2;} HR{color : #0086b2;}
--></STYLE> </head><body>
<h1><%=company%> Reports - HTTP Status 302 - Moved Temporarily</h1>
<HR size="1" noshade><p><b>type</b> Status report</p>
<p><b>message</b> <u>Moved Temporarily</u></p>
<p><b>description</b>
<u>The requested resource (Moved Temporarily) has moved temporarily
to a new location.</u></p>
<HR size="1" noshade>
</body></html>

<%
}
%>
