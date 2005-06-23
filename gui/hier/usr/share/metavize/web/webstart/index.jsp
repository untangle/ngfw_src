<%@ page language="java" %>

<%

String host=request.getHeader("host");
String scheme=request.getScheme();
String ctxPath=request.getContextPath();
String cb = scheme + "://" + host + ctxPath + "/";
String pageName = request.getServletPath();

boolean isIndex    = pageName.equals("/index.html");
boolean isHelp     = pageName.equals("/help.html");
boolean isDownload = pageName.equals("/download.html");
boolean isSecure   = scheme.equals("https");

/* If they request anything else, give them the index page */
if (!( isIndex || isHelp || isDownload)) isIndex = true;

String helpClickHere = "Click <a href=\"help.html\">here</a> for more information.";

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <% if ( isIndex ) { %>
    <script language="JavaScript" type="text/javascript">
      <!-- // Hide script from older browsers -->
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

        function showMessage()
        {
          if ( javaws150Installed == 0 && ( navigator.userAgent.indexOf("Gecko") == -1 )) {
            document.write( 'Java Web Start v1.5 was not detected.&nbsp; <%= helpClickHere %><br/><br/><div style="text-align: center;"><a href="download.html">Install Java Web Start v1.5</a></div><br/><br/>' );
          }
        }
     <!-- //  -->
    </script> 
    
    <script language="VBScript" type="text/vbscript">
      <!-- // Hide script from older browsers
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
        // -->
    </script>
    <% } // if ( isIndex ) %>

    <meta content="text/html; charset=ISO-8859-1" http-equiv="content-type"/>
    <title>
      EdgeGuard Client
    </title>
  </head>
  <body LINK="#0000EE" VLINK="#0000EE" style="background-image: url(images/DarkBackground1600x100.jpg);">
    <br/>
    <br/>
    <br/>
    <br/>
    <center>
      <table
          style="margin-left: auto; margin-right: auto; text-align: left; background-image: url(images/Background1600x100.jpg);"
          cellpadding="40" cellspacing="0" border="2">
        <tbody>
          <tr>
            <td style="vertical-align: top; font-family: helvetica,arial,sans-serif; width: 400px;">
              <div style="text-align: center;">
                <img alt="" src="images/LogoNoText96x96.gif" 
                     style="border: 0px solid ; width: 96px; height: 96px;" align="top"
                     hspace="0" vspace="0"/>
                <br/>
                <br/>
                <span style="font-weight: bold;">
                  Metavize EdgeGuard
                </span>
                <br/>
                <br/>
                <br/>
                <% if ( isDownload ) { %>
                <object codebase="http://java.sun.com/update/1.5.0/jinstall-1_5_0_03-windows-i586.cab"
                        classid="clsid:5852F5ED-8BF4-11D4-A245-0080C6F74284"
                        height="0"
                        width="0">
                  <param name="type" value="application/x-java-applet"/>
                  <param name="app" value="<%=cb%>gui.jnlp"/>
                  <param name="back" value="true"/>
                  <!-- Alternate HTML for browsers which cannot instantiate the object -->
                  <a href="http://java.sun.com/cgi-bin/javawebstart-platform.sh?">
                    Download Java Web Start
                  </a>
                </object>
                <% } else { // if ( isDownload ) %>
                Server: 
                <span style="font-style: italic;">
                  <%=host %>
                </span>
		<br/>
		Connection:
		<span style="font-style: italic;">
                  <% if(isSecure){  %>
			https (secure)
		<%} else {%>
			http (standard)
                <% } %>
                </span>
              </div>
              <% if ( !isDownload ) { %>
              <br/>
              <br/>
              <% } // if ( !isDownload )
              if ( isIndex ) { %>
              <script type="text/javascript" language="Javascript">
                <!-- 
                     showMessage();
                -->
              </script>
              <noscript>
                Because Javascript is disabled, the latest version
                of Java Web Start could not be detected.&nbsp; 
                <%= helpClickHere %>
                <br/>
                <br/>
                <div style="text-align: center;">
                  <a href="download.html">Install Java Web Start v1.5</a>
                </div>
                <br/>
                <br/>
              </noscript>
              <% } else if ( isHelp ) { %>
              The latest version
              of Java Web Start could not be detected.&nbsp; 
              <br/>
              <br/>
              <ul>
                <li>
                  This software requires Java Web Start v1.5 to execute.
                </li>
                <li>
                  If you have the latest version of Java Web Start
                  installed, please click "Launch EdgeGuard Client"
                </li>
                <li>
                  If you are unsure of which version of Java Web Start
                  you have installed please click "Install Java Web Start v1.5"
                </li>
              </ul>
              <div style="text-align: center;">
                <a href="download.html">Install Java Web Start v1.5</a>
              </div>
              <br/>
              <br/>
              <% } // else if ( isHelp )
              if ( !isDownload ) { %>
              <div style="text-align: center;">
                <a href="gui.jnlp">Launch EdgeGuard Client</a><br>

		<br><a href="<%=scheme%>://<%=host%>/reports">View EdgeReport Reports</a>

		<% } %>
              </div>
              <% } %>
            </td>
          </tr>
        </tbody>
      </table>
    </center>
    <br/>
    <br/>
    <br/>    
  </body>
</html>
