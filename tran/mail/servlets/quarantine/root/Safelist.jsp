<%@page language="java" import="com.untangle.mvvm.*"%>

<%
/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
%>
<%@ taglib uri="/WEB-INF/taglibs/quarantine_euv.tld" prefix="quarantine" %>

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
  <title><%=company%> | Safelist for <quarantine:currentAddress/></title>
    <script>
      function CheckAll() {
        count = document.form1.elements.length;
        isOn = document.form1.checkall.checked;
        for (i=0; i < count; i++) {
          document.form1.elements[i].checked = isOn;
        }
      }
    </script>
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
        Safelist for:<br/><quarantine:currentAddress/>
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
        Welcome to your &quot;safelist.&quot;
        A safelist is a list of email addresses,
        usually email addresses of your friends and colleagues.
        Emails, sent by anyone on a safelist, will <i>not</i> be quarantined even if the emails were to be identified as spam or phish.<br><br>
        To delete any email address on the safelist,
        click the checkboxes for one or more email addresses and click <code>Remove Selected Addresses</code>.
        To add an email address to the safelist,
        enter the email address in the text field and click <code>Submit</code>.<br><br>

        <!-- INITIAL TABLE -->
        <center>
        <table>
              <quarantine:hasMessages type="info">
              <tr>
                <td>
                  <ul class="messageText">
                    <quarantine:forEachMessage type="info">
                      <li><quarantine:message/></li>
                    </quarantine:forEachMessage>
                  </ul>
                </td>
              </tr>
              </quarantine:hasMessages>
        </table>
        </center>
        <center>
        <table>
              <quarantine:hasMessages type="error">
              <tr>
                <td>
                  <ul class="errortext">
                    <quarantine:forEachMessage type="error">
                      <li><quarantine:message/></li>
                    </quarantine:forEachMessage>
                  </ul>
                </td>
              </tr>
              </quarantine:hasMessages>
        </table>
        </center>

        <!-- INPUT FORM 1 -->
          <form name="form1" method="POST" action="safelist">
            <input type="hidden"
              name="<quarantine:constants keyName="action"/>"
              value="<quarantine:constants valueName="slremove"/>"/>
            <input type="hidden"
              name="<quarantine:constants keyName="tkn"/>"
              value="<quarantine:currentAuthToken encoded="false"/>"/>
            <table border="0" cellpadding="0" cellspacing="0" width="100%">
              <tr>
                <td>
                  <table class="slist" width="100%">
                    <thead>
                      <tr>
                        <th><input type="checkbox"
                          name="checkall"
                          value="checkall"
                          onclick="CheckAll()"></th>
                        <th width="100%">Email Address</th>
                      </tr>
                    </thead>
                    <tbody>
                      <quarantine:forEachSafelistEntry>
                        <tr>
                          <td><input type="checkbox"
                            name="<quarantine:constants keyName="sladdr"/>"
                            value="<quarantine:safelistEntry encoded="false"/>"/></td>
                          <td><quarantine:safelistEntry encoded="false"/></td>
                        </tr>
                      </quarantine:forEachSafelistEntry>
                    </tbody>
                    <tfoot>
                      <tr>
                        <td colspan="2"><input type="Submit" value="Remove Selected Addresses"/></td>
                      </tr>
                    </tfoot>
                  </table>
                </td>
              </tr>
            </table>
          </form>

      <!-- INPUT FORM 2 -->
          <br><br>
          <form name="form2" action="safelist">
            <input type="hidden"
              name="<quarantine:constants keyName="action"/>"
              value="<quarantine:constants valueName="sladd"/>"/>
            <input type="hidden"
              name="<quarantine:constants keyName="tkn"/>"
              value="<quarantine:currentAuthToken encoded="false"/>"/>
            <table>
              <tr>
                <td class="enteremail">
                  <table>
                    <tr>
                      <td>Add address: </td>
                      <td><input type="text" name="<quarantine:constants keyName="sladdr"/>"/>
                    </tr>
                    <tr><td colspan="2"><input type="submit" name="Submit" value="Submit"/></td></tr>
                  </table>
                </td>
              </tr>
            </table>
          </form>


        <br/>
    <center>Powered by Untangle&trade; Server</center>

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

