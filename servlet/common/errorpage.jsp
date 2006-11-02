<%@ page isErrorPage="true" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<%
Object errorObject = request.getAttribute( "com.untangle.mvvm.util.errorpage.error-message" );
String errorMessage = "Access to this resource is prohibited.";
if (( errorObject != null ) && ( errorObject instanceof String )) {
  errorMessage = (String)errorObject;
}

%>

<html>
  <head>
    <meta content="text/html; charset=ISO-8859-1" http-equiv="content-type"/>
    <title>
        Untangle Networks Platform
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
                  Untangle Networks Platform
                </span>
                <br/>
                <br/>
                <br/>
                <span style="font-weight: italic;">
                  <%= errorMessage %><br>
                </span>
              </div>
              <br/>
              <br/>
              <br/>
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
