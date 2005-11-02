<!--
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
-->
<%@ taglib uri="/WEB-INF/taglibs/quarantine_euv.tld" prefix="quarantine" %>

<html>
  <link rel="StyleSheet" href="styles/mv_quarantine.css" type="text/css">
  <head>
    <title>
    Request Quarantine Digest Email
    </title>
  </head>
<body>

<div align="center">
<TABLE align="center" HEIGHT="100%" WIDTH="100%" BORDER="0" CELLPADDING="0" CELLSPACING="0">
  <TR>
    <TD>
      <TABLE WIDTH="100%" height="100%" BORDER="0" CELLPADDING="0" CELLSPACING="0" bgcolor="white" align="center">
        <TR>
          <TD valign="top"><IMG SRC="images/tl.gif" WIDTH="20" HEIGHT="20"></TD>
          <td>
            <table class="intro" HEIGHT="100%" WIDTH="100%" BORDER="0" CELLPADDING="0" CELLSPACING="0">
              <tr>
                <td>
                  <table class="logoTable">
                    <tr>
                      <td>
                        <img src="images/logo.gif"/>
                      </td>
                      <td class="logotext">
                        &nbsp;&nbsp;Metavize EdgeGuard
                      </td>
                    </tr>
                  </table>
                </td>
                <td>
                  <table class="introUserInfo" height="100%" width="100%"  border="0" cellpadding="0" cellspacing="0">
                    <tbody>
                      <tr>
                        <td>
                          Request Quarantine Digest Email
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </td>
              <tr>
              <tr>
                <td class="infotext" colspan="2">
                <p>This page is used to request an email to your inbox, listing any quarantined emails.</p><br>
                <p>Please enter your email address into the form below.  You will then receive an email with links for you to view your quarantined messages.  You can then release or delete any of your quarantined messages.
                </p>
                  <br><br><br><br>
                </td>
              </tr>
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
          </td>
          <TD valign="top" align="right"><IMG SRC="images/tr.gif" WIDTH="20" HEIGHT="20"></TD>
        </TR>
        <tr>
          <td>
          </td>
          <td>
            <form name="form1" method="POST" action="rdc">
              <table>
                <tr><td class="enteremail">
                  <table>
                    <tr>
                      <td>Email Address:&nbsp;
                      </td>
                      <td>
                        <input type="text" name="<quarantine:constants keyName="draddr"/>"/>
                      </td>
                    </tr>
                    <tr>
                      <td class="paddedButton" colspan="2" align="center">
                        <input type="submit" value="submit"/>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </form>
          </td>
          <td>
          </td>
        </tr> 
        <TR>
        <TD valign="bottom"><IMG SRC="images/bl.gif" WIDTH="20" HEIGHT="20"></TD>
        <td class="smallLogo" height="100%">
          Powered by <br>
          Metavize EdgeGuard
        </td>
        <TD valign="bottom" align="right"><IMG SRC="images/br.gif" WIDTH="20" HEIGHT="20"></TD>
      </TR>
    </TABLE>
  </TD>
</TR>
</TABLE>
</div>
<br><br><br><br>

</body>
</html>
