<%@ page isErrorPage="true" import="com.untangle.mvvm.LocalAppServerManager,org.apache.log4j.Logger,org.apache.catalina.Globals,org.apache.catalina.valves.Constants,org.apache.catalina.util.StringManager"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<%
// See LocalStrings.properties in the apache package for message
// format. XXX we could make our own friendlier version.

Object o = request.getAttribute(LocalAppServerManager.MVVM_WEB_MESSAGE_ATTR);
String errorMessage = null != o && o instanceof String ? (String)o : null;

if (null == errorMessage) {
    ErrorData ed = pageContext.getErrorData();
    Throwable t = null == ed ? null : ed.getThrowable();
    if (null == t) {
        o = request.getAttribute(Globals.STATUS_CODE_ATTR);
        int statusCode = null != o && o instanceof Integer ? (Integer)o : 0;
        o = request.getAttribute(Globals.ERROR_MESSAGE_ATTR);
        String msg = null != o && o instanceof String
            ? (String)o
            : "Access to this resource is prohibited.";
        StringManager sm = StringManager.getManager(Constants.Package);
        errorMessage = sm.getString("http." + statusCode, msg);
    } else {
        Logger.getLogger(getClass()).error("Exception thrown in servlet", t);
        errorMessage = "Unexpected Error";
    }
}
%>

<html>
  <head>
    <meta content="text/html; charset=ISO-8859-1" http-equiv="content-type"/>
    <title>
        Untangle Server
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
                  Untangle Server
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
