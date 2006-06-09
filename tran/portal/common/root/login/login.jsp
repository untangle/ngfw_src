<%@ page language="java" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<%
String cp = (String)request.getContextPath();
String host=request.getHeader("host");
%>

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<title>Metavize Portal Login</title>
</head>


<body LINK="#0000EE" VLINK="#0000EE" style="background-image: url(<%=cp%>/login/DarkBackground1600x100.jpg);">

    <br/>
    <br/>
    <br/>
    <br/>

    <center>
      <table
          style="margin-left: auto; margin-right: auto; text-align: left; background-image: url(<%=cp%>/login/Background1600x100.jpg);"
          cellpadding="40" cellspacing="0" border="2">
        <tbody>
          <tr>
            <td style="vertical-align: top; font-family: helvetica,arial,sans-serif; width: 400px;">
              <div style="text-align: center;">
                <img alt="" src="<%=cp%>/login/LogoNoText96x96.gif"
                     style="border: 0px solid ; width: 96px; height: 96px;" align="top"
                     hspace="0" vspace="0"/>
                <br/>
                <br/>
                <span style="font-weight: bold;">
                  Metavize EdgeGuard Portal
                </span>
                <br/>
                <br/>
                  <center>
                    <table border="0">
                      <tr>
                        <td align="right">Server:</td>
                        <td><i>&nbsp;<%=host %></i></td>
                      </tr>

			<form method="POST" action="j_security_check">
			<tr><td align="right">Login:</td><td><input type="text" name="j_username"></td></tr>
			<tr><td align="right">Password:</td><td><input type="password" name="j_password"></td></tr>
                    </table>	
			<br/>
			<input type="submit" value="login">
			</form>
			<br/>
			<br/>
			<br/>
			<div style="font-style: italic; font-size: 80%;"> 	
			<a href="http://<%=host%>/webstart">Administrator Login</a>
			</div>
                  </center>
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