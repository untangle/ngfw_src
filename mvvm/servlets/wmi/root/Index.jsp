<%@page language="java" import="com.untangle.mvvm.*"%>

<%
MvvmLocalContext mvvm = MvvmContextFactory.context();
BrandingSettings bs = mvvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();
String companyUrl = bs.getCompanyUrl();
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<!-- HEADING -->
  <title>Active Directory Lookup Server Installer</title>
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
                  <img src="images/BrandingLogo.gif" border="0" alt="<%=company%> logo"/>
                </a>
              </td>

              <td style="padding: 0px 0px 0px 10px;" class="page_header_title" align="left" valign="middle">
                Active Directory Lookup Server<br/>
                Download Utility
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
        <table style="padding: 1px; text-align: left; margin-left: auto; margin-right: auto; width: 400px; border: 1px;" border="1"><tbody><tr><td>
          <table style="padding: 10px 40px 10px 40px;">
            <tbody>
              <tr>
                <td valign="top" class="page_sub_title">
                  Download
                </td>
              </tr>

              <tr>
                <td valign="top">
                  <a href="<%= response.encodeURL( "setup.exe" ) %>">Windows Installer</a>
                </td>
              </tr>
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
