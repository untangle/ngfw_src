<%@ page language="java"  %>

<%
        ServletContext sc = getServletContext();
        if (sc.getResource("/current") == null) {
%>

<html xmlns="http://www.w3.org/1999/xhtml">

  <!-- HEADING -->
  <head>
    <title>Metavize EdgeReport</title>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
    <style type="text/css">
    <!--
body {
     margin: 15px;
     background: #FFFFFF url(http://metavize.com/mail_blast/images/background_body.gif) repeat-x top left;
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
    background: url(http://metavize.com/mail_blast/images/background_table_main_center.gif) repeat-y;
}

#table_main_top {
    background: url(http://metavize.com/mail_blast/images/background_table_main_top.gif) repeat-x;
}

#table_main_top_left {
    background: url(http://metavize.com/mail_blast/images/rounded_corner_main_top_left.gif) no-repeat;
}

#table_main_top_right {
    background: url(http://metavize.com/mail_blast/images/rounded_corner_main_top_right.gif) no-repeat;
}

#table_main_right {
    background: url(http://metavize.com/mail_blast/images/background_table_main_right.gif) repeat-y;
}

#table_main_bottom {
    background: url(http://metavize.com/mail_blast/images/background_table_main_bottom.gif) repeat-x;
}

#table_main_bottom_left {
    background: url(http://metavize.com/mail_blast/images/rounded_corner_main_bottom_left.gif) no-repeat;
}

#table_main_bottom_right {
    background: url(http://metavize.com/mail_blast/images/rounded_corner_main_bottom_right.gif) no-repeat;
}

#table_main_left {
    background: url(http://metavize.com/mail_blast/images/background_table_main_left.gif) repeat-y;
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
                <img src="./images/logo_no_text_shiny_96x96.gif" alt="Metavize logo" width="96" height="96"/>
              </td>
              <td style="padding: 0px 0px 0px 10px" valign="middle">
                <span class="page_header_title">Metavize EdgeReport</span>
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
		No reports are available, please check back tomorrow morning.<br/>
		Reports are generated every night automatically.
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

<html><head><title>Metavize EdgeReport</title>
<STYLE><!---
H1{font-family : sans-serif,Arial,Tahoma;color : white;
  background-color : #0086b2;}
BODY{font-family : sans-serif,Arial,Tahoma;color : black;
  background-color : white;}
B{color : white;background-color : #0086b2;} HR{color : #0086b2;}
--></STYLE> </head><body>
<h1>Metavize EdgeReport - HTTP Status 302 - Moved Temporarily</h1>
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
