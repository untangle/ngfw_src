<%
/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
%>
<%@ taglib uri="/WEB-INF/taglibs/quarantine_euv.tld" prefix="quarantine" %>

<html>
<link rel="StyleSheet" href="styles/mv_quarantine.css" type="text/css">
  <head>
    <title>
    Safelist for <quarantine:currentAddress/>
    </title>
    <script> 
      function CheckAll() {
        count = document.form1.elements.length;
        isOn = document.form1.checkall.checked;
        for (i=0; i < count; i++) {
          document.form1.elements[i].checked = isOn;
        }
      }
    </script>
  </head>
  <body>
    <table class="outertable" cellpadding="0" cellspacing="0">
      <% // Outer table, first row %>
      <tr>
        <% // Because of IE stupidity, there can be no LWS between the tags below %>
        <td WIDTH="20" valign="top"><img src="images/tl.gif" ALT=""></td>
        <td>
          <table class="intro"
            border="0"
            cellpadding="0"
            cellspacing="0"
            height="100%"
            width="100%">
              <tbody>
                <tr>
                  <% // I cannot get things to work correctly w/o putting an absolute value here %>
                  <td width="200">
                    <% // IE ignores this class if applied to the table below %>
                    <div class="logoTable">
                      <table>
                        <tbody>
                          <tr>
                            <td>
                              <img src="images/logo.gif">
                            </td>
                            <td class="logotext">
                            &nbsp;&nbsp;Metavize EdgeGuard
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </td>
                  <td>
                    <table class="introUserInfo" height="100%" width="100%">
                      <tbody>
                        <tr>
                          <td>
                            Safelist for <quarantine:currentAddress/>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </td>
                </tr><tr>
                </tr><tr>
                <td class="infotext" colspan="2">
Welcome to your &quot;safelist&quot;.  A safelist is a list of email addresses, usualy your
friends and collegues.  Emails from people on this list will <i>not</i> be considered spam.<br><br><br><br>
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
            </tbody>
          </table>
        </td>
        <% // Because of IE stupidity, there can be no LWS between the tags below %>
        <td WIDTH="20" valign="top"><img src="images/tr.gif"></td>
      </tr>
      <% // Outer table, middle row %>
      <tr>
        <td>
        </td>
        <td>
          <form name="form1" method="POST" action="smc">
            <input type="hidden"
              name="<quarantine:constants keyName="action"/>"
              value="<quarantine:constants valueName="slremove"/>"/>
            <input type="hidden"
              name="<quarantine:constants keyName="tkn"/>"
              value="<quarantine:currentAuthToken encoded="false"/>"/>
            <table width="100%">
              <tr>
                <td>
                  <table class="slist">
                    <thead>
                      <tr>
                        <th><input name="checkall"
                          value="checkall"
                          onclick="CheckAll()"
                          type="checkbox"></th>
                        <th width="100%">Name</th>
                      </tr>
                    </thead>
                    <tbody>
                      <quarantine:forEachSafelistEntry>
                        <tr>
                          <td>
                            <input type="checkbox"
                              name="<quarantine:constants keyName="sladdr"/>"
                              value="<quarantine:safelistEntry encoded="false"/>"/>
                          </td>
                          <td>
                            <quarantine:safelistEntry encoded="false"/>
                          </td>
                        </tr>
                      </quarantine:forEachSafelistEntry>                  
                    </tbody>
                    <tfoot>
                      <tr>
                        <td colspan="2"><input type="Submit" value="Remove Selected Addresses"/>
                        </td>
                      </tr>
                    </tfoot>
                  </table>
                </td>
              </tr>
            </table>
          <br><br>
          <form name="form2" action="smc">
            <input type="hidden"
              name="<quarantine:constants keyName="action"/>"
              value="<quarantine:constants valueName="sladd"/>"/>
            <input type="hidden"
              name="<quarantine:constants keyName="tkn"/>"
              value="<quarantine:currentAuthToken encoded="false"/>"/>
            <table>
              <tr>
                <td>Add address: </td>
                <td><input type="text" name="<quarantine:constants keyName="sladdr"/>"
              </tr>
              <tr><td colspan="2"><input type="submit" name="Submit" value="Submit"/></td></tr>
            </table>
          </form>          
        </td>
        <td>
        </td>
      </tr>
      <% // Outer table, last row %>
      <tr>
        <% // Because of IE stupidity, there can be no LWS between the tags below %>
        <td WIDTH="20" valign="bottom"><img src="images/bl.gif" ALT=""></td>
        <td class="smallLogo" height="100%">
          Powered by <br>
          Metavize&reg; EdgeGuard&reg;
        </td>
        <% // Because of IE stupidity, there can be no LWS between the tags below %>
        <td WIDTH="20" valign="bottom"><img src="images/br.gif" ALT=""></td>
      </tr>
    </table>
    
  </body>
</html>