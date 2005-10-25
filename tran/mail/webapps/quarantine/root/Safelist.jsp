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
        </td>
        <% // Because of IE stupidity, there can be no LWS between the tags below %>
        <td WIDTH="20" valign="top"><img src="images/tr.gif"></td>
      </tr>
      <% // Outer table, middle row %>
      <tr>
        <td>
        </td>
        <td>
          This page is temp, just here to show functionality.
          <quarantine:hasMessages type="info">
            <ul class="messageText">
              <quarantine:forEachMessage type="info">
                <li><quarantine:message/></li>
              </quarantine:forEachMessage>
            </ul>
          </quarantine:hasMessages>
          <quarantine:hasMessages type="error">
            <ul class="errortext">
              <quarantine:forEachMessage type="error">
                <li><quarantine:message/></li>
              </quarantine:forEachMessage>
            </ul>
          </quarantine:hasMessages>

          <br><br>
          <form name="form1" method="POST" action="smc">
            <input type="hidden"
              name="<quarantine:constants keyName="action"/>"
              value="<quarantine:constants valueName="slremove"/>"/>
            <input type="hidden"
              name="<quarantine:constants keyName="tkn"/>"
              value="<quarantine:currentAuthToken encoded="false"/>"/>
            <table>
              <tr>
                <td>
                  <input name="checkall"
                    value="checkall"
                    onclick="CheckAll()"
                    type="checkbox">
                </td>
                <td>
                  <input type="submit" name="Delete" value="Delete"/>
                </td>
              </tr>
              <quarantine:forEachSafelistEntry>
                <tr class="<quarantine:oddEven even="even" odd="odd"/>">
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
            </table>
          </form>          
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