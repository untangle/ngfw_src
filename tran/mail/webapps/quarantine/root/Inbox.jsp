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
    Quarantine Digest for <quarantine:currentAddress/>
    </title>  
    <script> 
    function CheckAll() {
      count = document.form1.elements.length;
      isOn = document.form1.checkall.checked;
      for (i=0; i < count; i++) {
        document.form1.elements[i].checked = isOn;
      }
    }
    
    function doPurge() {
      document.form1.<quarantine:constants keyName="action"/>.value = "<quarantine:constants valueName="purge"/>";
      document.form1.submit();
    }
    
    function doRescue() {
      document.form1.<quarantine:constants keyName="action"/>.value = "<quarantine:constants valueName="rescue"/>";
      document.form1.submit();
    }    
    </script>
</head>
<body>


<br><br>

<div align="center">
<TABLE align="center" HEIGHT="100%" WIDTH="100%" BORDER="0" CELLPADDING="0" CELLSPACING="0">
  <TR>
    <TD>
      <TABLE WIDTH="100%" height="100%" BORDER="0" CELLPADDING="0" CELLSPACING="0" bgcolor="white" align="center">
        <TR>
          <TD valign="top">
            <IMG SRC="images/tl.gif" WIDTH="20" HEIGHT="20">
          </TD>
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
                          Quarantine Digest for <quarantine:currentAddress/>
                        </td>
                      </tr>
                    </tbody>
                  </table>                
                </td>
              <tr>
              <tr>
                <td class="infotext" colspan="2">
                The emails listed below have been quarantined by your Metavize EdgeGuard appliance.  They will be deleted automatically after 20 days.  To release any email from the quarantine and have them delivered to your inbox, select the emails and click Release.  To delete emails, select the emails to be deleted and click Delete.  
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
          <TD valign="top" align="right">
            <IMG SRC="images/tr.gif" WIDTH="20" HEIGHT="20">
          </TD>
        </TR>
        <tr>
          <td>
          </td>
          <td>
          <quarantine:hasInboxRecords includeIfTrue="true">
            <form name="form1" action="imc">
              <input type="hidden"
                name="<quarantine:constants keyName="action"/>"
                value="<quarantine:constants valueName="viewibx"/>"/>
              <input type="hidden"
                name="<quarantine:constants keyName="tkn"/>"
                value="<quarantine:currentAuthToken encoded="false"/>"/>
              <table BORDER="0" CELLPADDING="0" CELLSPACING="0" width="100%">
                <tr>
                  <td>
                    <table width="100%" class="actions">
                      <tr>
                        <td>
                          <input type="button" name="Release" value="Release" onclick="doRescue()"/>&#32;&#32;
                          <input type="button" name="Delete" value="Delete" onclick="doPurge()"/>
                        </td>
                        <td>
                          <a href="http://www.google.com">Help</a>
                        </td>                                
                      </tr>
                    </table>
                  </td>
                </tr>
                <tr>
                  <td>
                    <table WIDTH="100%" class="inbox">
                      <thead>
                        <tr>
                          <th class="first" scope="col"><input type="checkbox"
                            name="checkall"
                            value="checkall"
                            onclick="CheckAll()"/></th>
                          <th scope="col">From</th>
                          <th scope="col">Score</th>
                          <th scope="col">Subject</th>
                        </tr>
                      </thead>
                      <tfoot>
                        <tr>
                          <th scope="row">Total</th>
                          <td colspan="4"><quarantine:indexMsgCount/> mails</td>
                        </tr>
                      </tfoot>
      
                      <tbody>
                        <quarantine:forEachInboxRecord>
                          <tr class="<quarantine:oddEven even="even" odd="odd"/>">
                            <th scope="row">
                              <input type="checkbox"
                                name="<quarantine:constants keyName="mid"/>"
                                value="<quarantine:inboxRecord prop="mid"/>"/>
                            </th>
                            <td><quarantine:inboxRecord prop="from"/></td>
                            <td><quarantine:inboxRecord prop="detail"/></td>
                            <td><quarantine:inboxRecord prop="subject"/></td>
                          </tr>
                        </quarantine:forEachInboxRecord>
                      </tbody>
                    </table>
                  </td>
                </tr>
              </table>
            </form>
          </quarantine:hasInboxRecords>
          </td>
          <td></td>
        </tr> 
        <TR>
        <TD valign="bottom">
          <IMG SRC="images/bl.gif" WIDTH="20" HEIGHT="20">
        </TD>
        <td class="smallLogo" height="100%">
          Powered by <br>
          Metavize EdgeGuard
        </td>
        <TD valign="bottom" align="right">
          <IMG SRC="images/br.gif" WIDTH="20" HEIGHT="20">
        </TD>
      </TR>
    </TABLE>
  </TD>
</TR>
</TABLE>
</div>
<br><br><br><br>

</body>
</html>