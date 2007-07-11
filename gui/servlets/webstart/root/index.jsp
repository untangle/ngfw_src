<%@ page language="java" import="com.untangle.uvm.*" %>
<%--
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--%>
<%
LocalUvmContext uvm = LocalUvmContextFactory.context();
boolean reportingEnabled = uvm.reportingManager().isReportingEnabled();
String host=request.getHeader("host");
String scheme=request.getScheme();
String ctxPath=request.getContextPath();
String cb = scheme + "://" + host + ctxPath + "/";
String pageName = request.getServletPath();

boolean isIndex = pageName.equals("/index.html");
boolean isDownload = pageName.equals("/download.html");
boolean isSecure   = scheme.equals("https");

/* If they request anything else, give them the index page */
if (!( isIndex || isDownload)) isIndex = true;
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Untangle Client Launcher</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
<% if ( isIndex ) { %>
<script type="text/javascript">
// <![CDATA[
var javawsInstalled    = 0;
var javaws142Installed = 0;
var javaws150Installed = 0;
isIE = "false";
if (navigator.mimeTypes && navigator.mimeTypes.length) {
    x = navigator.mimeTypes['application/x-java-jnlp-file'];
    if (x) {
        javawsInstalled = 1;
        javaws142Installed=1;
        javaws150Installed=1;
    }
}
else {
  isIE = "true";
}

function showMessage() {
  if ( javaws150Installed == 0 && ( navigator.userAgent.indexOf("Gecko") == -1 )) {
     document.write( 'Java&trade; v1.5 was not detected.  You may need to download and install Java&trade; v1.5.<br /><br />' );
  }
}
// ]]>
</script>
<!--[if lt ie 9]>
<script type="text/vbscript">
    on error resume next
    If isIE = "true" Then
            If Not(IsObject(CreateObject("JavaWebStart.isInstalled"))) Then
                    javawsInstalled = 0
            Else javawsInstalled = 1
            End If
            If Not(IsObject(CreateObject("JavaWebStart.isInstalled.1.4.2.0"))) Then
                    javaws142Installed = 0
            Else javaws142Installed = 1
            End If
            If Not(IsObject(CreateObject("JavaWebStart.isInstalled.1.5.0.0"))) Then
                    javaws150Installed = 0
            Else javaws150Installed = 1
            End If
    End If
</script>
<![endif]-->
<% } // if ( isIndex ) %>
</head>
<body>
<div id="main" class="main-reduced">
 <!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->

      <center>
            <img alt="Untangle" src="/images/BrandingLogo.gif" />

            <div style="margin: 0 auto; width: 250px; padding: 40px 0 5px;">
<% if ( host.equalsIgnoreCase( "untangledemo.untangle.com" )  && !isDownload ) { %>

                <b>Login: untangledemo</b><br />
                <b>Password: untangledemo</b><br />

<% } %>

<% if ( isDownload ) { %>
            <object codebase="http://java.sun.com/update/1.6.0/jinstall-6-windows-i586.cab#Version=6,0,0,99"
                    classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"
                    height="0"
                    width="0">
              <param name="type" value="application/x-java-applet" />
              <param name="app" value="<%=cb%>gui.jnlp" />
              <param name="back" value="true" />
            </object>

            </div>
            <!-- Alternate HTML for browsers which cannot instantiate the object -->
            <a href="http://www.untangle.com/javainstaller.html"><b>Download Java&trade; v1.6</b></a>

       </center>
<% } %>

<% if ( !isDownload ) { %>

                    <b>Server:</b> <em>&nbsp;<%=host %></em><br />
                    <b>Connection:</b> <em>
                         <% if(isSecure){  %>
                           &nbsp;https (secure)
                         <% } else { %>
                           &nbsp;http (standard)
                         <% } %>
                           </em><br />
              </div>
      </center>
      <br />
<% } // if ( !isDownload ) %>

<% if ( isIndex ) { %>
      <script type="text/javascript">
       // <![CDATA[
             showMessage();
      // ]]>
      </script>
    <% if ( !isDownload ) { %>
            <center>
              <a href="gui.jnlp"><b>Launch Untangle Client</b></a>
              <% if (reportingEnabled) { %>
                    <br /><a href="<%=scheme%>://<%=host%>/reports"><b>View Untangle Reports</b></a><br />
              <% } %>
            </center>
    <% } %>
<% } %>


      <div class="java-download">
       <a href="/java/jre-6u1-windows-i586-p.exe">Download Java&trade; v1.6 (Offline)</a><br />
<% if ( !isDownload ) { %>
             <a href="download.html">Download Java&trade; v1.6 (Online)</a><br />
<% } %>
      </div>

 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>

</body>
</html>
