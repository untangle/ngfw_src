<%@page language="java" import="com.untangle.mvvm.*"%>

<%
MvvmLocalContext mvvm = MvvmContextFactory.context();
BrandingSettings bs = mvvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();
String companyUrl = bs.getCompanyUrl();

boolean isValid;
String debuggingMessages;
String commonName;

try {
isValid = (Boolean)request.getAttribute( Util.VALID_ATTR );
debuggingMessages = (String)request.getAttribute( Util.DEBUGGING_ATTR );
commonName = (String)request.getAttribute( Util.COMMON_NAME_ATTR );
if ( commonName == null ) {
  commonName = "";
  isValid = false;
}
} catch ( Exception e ) {
isValid = false;
debuggingMessages = "";
commonName = "";
/* If any of these occur there was an error processing the page, user is doing something wrong */
response.setStatus( HttpServletResponse.SC_FORBIDDEN );
}
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<!-- HEADING -->
  <title>OpenVPN</title>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>

  <link rel="stylesheet" href="style.css" type="text/css"/>
</head>
<body>

<center>
<table border="0" cellpadding="0" cellspacing="0" width="904">

<!-- TOP THIRD -->
  <tbody>
    <tr>
      <td id="table_main_top_left"><img src="images/spacer.gif" alt=" " height="23" width="23"/><br/>
      </td>

      <td id="table_main_top" width="100%"><img src="images/spacer.gif" alt=" " height="1" width="1"/><br/>
      </td>

      <td id="table_main_top_right"> <img src="images/spacer.gif" alt=" " height="23" width="23"/><br/>
      </td>
    </tr>

    <!-- END TOP THIRD -->

    <!-- MIDDLE THIRD -->
    <tr>
      <td id="table_main_left"><img src="images/spacer.gif" alt=" " height="1" width="1"/></td>

      <td id="table_main_center">
        <table width="100%">
          <tbody>
            <tr>
              <td valign="middle" width="150">
                <a href="<%=companyUrl%>">
                  <img src="images/BrandingLogo.gif" border="0" alt="<%=company%>"/>
                </a>
              </td>

              <td style="padding: 0px 0px 0px 10px;" class="page_header_title" align="left" valign="middle">
                OpenVPN<br/>
                Client Download Utility
              </td>
            </tr>
          </tbody>
        </table>
      </td>

      <td id="table_main_right"> <img src="images/spacer.gif" alt=" " height="1" width="1"/></td>
    </tr>

    <!-- END MIDDLE THIRD --><!-- CONTENT AREA -->
    <tr>

      <td id="table_main_left"></td>

      <!-- CENTER CELL -->
      <td id="table_main_center" style="padding: 8px 0px 0px;">
        <hr size="1" width="100%"/>
        <table style="padding: 1px; text-align: left; margin-left: auto; margin-right: auto; width: 400px; border: 1px;" border="0"><tbody><tr><td>
          <table style="padding: 10px 40px 10px 40px;">
            <tbody>
              <% if ( isValid ) { %>
              <tr>
                <td valign="top" class="page_sub_title">
                  Download
                </td>
              </tr>
              <tr>
                <td valign="top">
                  Common Name:   <%= commonName %><br/>
                  Please select one of the following files.
                </td>
              </tr>

              <tr>
                <td valign="top">
                  <a href="<%= response.encodeURL( "setup.exe" ) %>">Windows Installer</a>
                </td>
              </tr>

              <tr>
                <td valign="top">
                  <a href="<%= response.encodeURL( "config.zip" ) %>">Configuration Files</a>
                </td>
              </tr>
              <% } else { // if ( isValid ) %>
              <tr>
                <td valign="top" class="page_sub_title">
                  Warning
                </td>
              </tr>
              <tr>
                <td valign="top">
                  The files that you requested are no longer available,
                  please contact your network administrator for more information.
                </td>
              </tr>
              <% } // else { if ( isValid ) %>
            </tbody>
          </table>
          </td></tr></tbody></table>
          <hr size="1" width="100%"/>
        </td>
      <!-- END CENTER CELL -->
      <td id="table_main_right"></td>
    </tr>
    <!-- END CONTENT AREA -->

    <!-- BOTTOM THIRD -->
    <tr>
      <td id="table_main_bottom_left"><img src="images/spacer.gif" alt=" " height="23" width="23"/><br/>
      </td>
      <td id="table_main_bottom"><img src="images/spacer.gif" alt=" " height="1" width="1"/><br/>
      </td>
      <td id="table_main_bottom_right"> <img src="images/spacer.gif" alt=" " height="23" width="23"/><br/>
      </td>
    </tr>
    <!-- END BOTTOM THIRD -->
  </tbody>
</table>

</center>

<!-- END BRUSHED METAL PAGE BACKGROUND -->
</body>
</html>
