<%@ page language="java" import="com.untangle.mvvm.*" %>

<%

MvvmLocalContext mvvm = MvvmContextFactory.context();
String host=request.getHeader("host");
String scheme=request.getScheme();
String ctxPath=request.getContextPath();
String cb = scheme + "://" + host + ctxPath + "/";
String pageName = request.getServletPath();

boolean isIndex    = pageName.equals("/index.html");
boolean isDownload = pageName.equals("/download.html");
boolean isSecure   = scheme.equals("https");

String target = request.getParameter("t");

/* If they request anything else, give them the index page */
if (!( isIndex || isDownload)) isIndex = true;

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
            document.write( 'Java&trade; v1.5 was not detected.  You may need to download and install Java&trade; v1.5.<br/><br/>' );
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
      Remote Desktop Client
    </title>
  </head>
  <body LINK="#0000EE" VLINK="#0000EE" style="background-image: url(/images/DarkBackground1600x100.jpg);">
    <br/>
    <br/>
    <br/>
    <br/>
    <center>
      <table
          style="margin-left: auto; margin-right: auto; text-align: left; background-image: url(/images/Background1600x100.jpg);"
          cellpadding="40" cellspacing="0" border="2">
        <tbody>
          <tr>
            <td style="vertical-align: top; font-family: helvetica,arial,sans-serif; width: 400px;">
              <div style="text-align: center;">
                <img alt="" src="/images/Logo150x96.gif"
                     style="border: 0px solid ; width: 150px; height: 96px;" align="top"
                     hspace="0" vspace="0"/>
                <br/>
                <br/>
                <span style="font-weight: bold;">
                  Untangle VNC Portal
                </span>
                <br/>
                <br/>
                <% if ( host.equalsIgnoreCase( "untangledemo.untangle.com" )  && !isDownload ) { %>
                  <b>Login: untangledemo</b>
                  <br/>
                  <b>Password: untangledemo</b>
                  <br/>
                  <br/>
                <% } %>
                <br/>
                <% if ( isDownload ) { %>
                <object codebase="http://java.sun.com/update/1.5.0/jinstall-1_5_0_07-windows-i586.cab"
                        classid="clsid:5852F5ED-8BF4-11D4-A245-0080C6F74284"
                        height="0"
                        width="0">
                  <param name="type" value="application/x-java-applet"/>
                  <param name="app" value="<%=cb%>vnc.jnlp?t=<%=target%>"/>
                  <param name="back" value="true"/>
                </object>
                <!-- Alternate HTML for browsers which cannot instantiate the object -->
                <a href="http://www.untangle.com/javainstaller.html">
                  Download Java&trade; v1.5
                </a>
                <% } %>
                <% if ( !isDownload ) { %>
                  <center>
                    <table border="0">
                      <tr>
                        <td align="right">Server:</td>
                        <td><i>&nbsp;<%=host %></i></td>
                      </tr>
                      <tr>
                        <td align="right">Connection:</td>
                        <td style="font-style: italic;">
                          <% if(isSecure){  %>
                            &nbsp;https (secure)
                          <% } else { %>
                            &nbsp;http (standard)
                          <% } %>
                        </td>
                      </tr>
                    </table>
                  </center>
              </div>
              <br/>
              <% } // if ( !isDownload )
              if ( isIndex ) { %>
              <script type="text/javascript" language="Javascript">
                <!--
                     showMessage();
                -->
              </script>
              <% if ( !isDownload ) { %>
                <div style="text-align: center;">
                  <a href="vnc.jnlp?t=<%=target%>">Launch VNC Remote Desktop Client</a><br>

                </div>
                <% } %>
              <% } %>

              <br/>
              <br/>
              <div style="text-align: right; font-style: italic; font-size: 80%;">
               <a href="/java/jre-1_5_0_07-windows-i586-p.exe">Download Java&trade; v1.5 (Offline)</a><br/>
               <% if ( !isDownload ) { %>
                 <a href="download.html">Download Java&trade; v1.5 (Online)</a><br/>
               <% } %>
              </div>
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
