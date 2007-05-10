<%@page language="java" import="com.untangle.mvvm.*"%>

<!--
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
-->
<%@ taglib uri="/WEB-INF/taglibs/quarantine_euv.tld" prefix="quarantine" %>

<%
MvvmLocalContext mvvm = MvvmContextFactory.context();
BrandingSettings bs = mvvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();
if (null == company) { company = "Untangle"; }
String companyUrl = bs.getCompanyUrl();
if (null == companyUrl) { companyUrl = "http://www.untangle.com"; }
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<!-- HEADING -->
  <title><%=company%> | Invalid Remote Access Portal Email</title>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
  <link rel="stylesheet" href="styles/style.css" type="text/css"/>
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
                Invalid Remote Access Portal Email
              </td>
            </tr>
          </tbody>
        </table>
      </td>

      <td id="table_main_right"> <img src="images/spacer.gif" alt=" " height="1" width="1"/></td>
    </tr>

    <!-- END MIDDLE THIRD -->
    <!-- CONTENT AREA -->
    <tr>

      <td id="table_main_left"></td>

      <!-- CENTER CELL -->
      <td id="table_main_center" style="padding: 8px 0px 0px;">
        <hr size="1" width="100%"/>

        <!-- INTRO MESSAGE -->
        Your Remote Access Portal login has been configured without an email address or with an incorrect email address.
        <br/><br/>
        If your company uses Active Directory, please contact your system administrator to configure your Active Directory Server account with your correct email address.
        <br/>
        If your company uses Local LDAP Directory, please contact your system administrator to configure your Local LDAP Directory account with your correct email address.
        <br/>
        Alternatively, please enter your email address into the form below and click <code>Submit</code>.
        You will receive an email message containing a list of your recent quarantined emails and a link to your quarantined email page.
        When you receive this email message, you can release or delete your quarantined emails from this list or access your quarantined email page from your browser to release or delete your quarantined emails.
        <br/>

        <!-- MAIN MESSAGE -->
        <br/>
        <center>
        <table>
              <quarantine:hasMessages type="info">
        <tr><td>
                  <ul class="messageText">
                    <quarantine:forEachMessage type="info">
                      <li><quarantine:message/></li>
                    </quarantine:forEachMessage>
                  </ul>
        </td></tr>
              </quarantine:hasMessages>
        </table>
        </center>

        <center>
        <table>
              <quarantine:hasMessages type="error">
        <tr><td>
                  <ul class="errortext">
                    <quarantine:forEachMessage type="error">
                      <li><quarantine:message/></li>
                    </quarantine:forEachMessage>
                  </ul>
        </td></tr>
              </quarantine:hasMessages>
        </table>
        </center>

        <!-- INPUT FORM -->
            <form name="form1" method="POST" action="requestdigest">
        <center>
                  <table style="border: 1px solid; padding: 10px">
                    <tr>
                      <td>Email Address:&nbsp;
                      </td>
                      <td>
                        <input type="text" name="<quarantine:constants keyName="draddr"/>"/>
                      </td>
                    </tr>
                    <tr>
                      <td class="paddedButton" colspan="2" align="center">
                        <input type="submit" value="Submit"/>
                      </td>
                    </tr>
                  </table>
        </center>
            </form>

        <br/>
    <center>Powered by Untangle&reg; Server</center>

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
