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

<!--
Prev link: <quarantine:pagnationLink linkType="prev"/>
Next link: <quarantine:pagnationLink linkType="next"/>

SEP
<quarantine:hasPagnation linkType="prev" includeIfTrue="true">
  LinkType="prev" includeIfTrue="true"
</quarantine:hasPagnation>
SEP
<quarantine:hasPagnation linkType="prev" includeIfTrue="false">
  LinkType="prev" includeIfTrue="false"
</quarantine:hasPagnation>
SEP
<quarantine:hasPagnation linkType="next" includeIfTrue="true">
  LinkType="next" includeIfTrue="true"
</quarantine:hasPagnation>
SEP
<quarantine:hasPagnation linkType="next" includeIfTrue="false">
  LinkType="next" includeIfTrue="false"
</quarantine:hasPagnation>

-->  

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
                            Quarantine Digest for <quarantine:currentAddress/>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </td>
                </tr><tr>
                </tr><tr>
                <td class="infotext" colspan="2">
  The emails listed below have been quarantined by your Metavize
  EdgeGuard appliance. They will be deleted automatically after 20 days.
  To release any email from the quarantine and have them delivered to
  your inbox, select the emails and click Release. To delete emails,
  select the emails to be deleted and click Delete. <br><br><br><br>                
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
          <quarantine:hasInboxRecords includeIfTrue="true">
          <form name="form1" method="POST" action="imc">
            <input type="hidden"
              name="<quarantine:constants keyName="action"/>"
              value="<quarantine:constants valueName="viewibx"/>"/>
            <input type="hidden"
              name="<quarantine:constants keyName="tkn"/>"
              value="<quarantine:currentAuthToken encoded="false"/>"/>
            <input type="hidden"
              name="<quarantine:constants keyName="sort"/>"
              value="<quarantine:pagnationProperties propName="sorting"/>"/>
            <input type="hidden"
              name="<quarantine:constants keyName="ascend"/>"
              value="<quarantine:pagnationProperties propName="ascending"/>"/>
            <input type="hidden"
              name="<quarantine:constants keyName="first"/>"
              value="<quarantine:pagnationProperties propName="thisId"/>"/>                            
            <table border="0" cellpadding="0" cellspacing="0" width="100%">
              <tbody>
                <tr>
                  <td>
                    <table class="actions"
                      border="0"
                      cellpadding="0"
                      cellspacing="0"
                      width="100%">
                      <tbody>
                        <tr>
                          <td>
                            <input name="Release" value="Release" onclick="doRescue()" type="button">  
                            <input name="Delete" value="Delete" onclick="doPurge()" type="button">
                          </td>
                          <td>
                            <div class="msiehack1">
                              <quarantine:hasPagnation linkType="prev" includeIfTrue="true">
                                <a href="/quarantine<quarantine:pagnationLink linkType="prev"/>">Prev</a>
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="prev" includeIfTrue="false">
                                Prev
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="next" includeIfTrue="true">
                                <a href="/quarantine<quarantine:pagnationLink linkType="next"/>">|Next</a>
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="next" includeIfTrue="false">
                                |Next
                              </quarantine:hasPagnation>
                            </div>
                          </td>                                
                       </tr>
                      </tbody>
                    </table>
                  </td>
                </tr>
                <tr>
                  <td>
                    <table class="inbox" width="100%">
                      <thead>
                        <tr>
                          <th class="first" scope="col">
                            <input name="checkall"
                              value="checkall"
                              onclick="CheckAll()"
                              type="checkbox">
                          </th>
                          <th scope="col">From</th>
                          <th scope="col">Score</th>
                          <th scope="col">Subject</th>
                        </tr>
                      </thead>
                      <tfoot>
                        <tr>
                          <td colspan="4">
                            <div class="tableFooter">
                              <quarantine:hasPagnation linkType="prev" includeIfTrue="true">
                                <a href="/quarantine<quarantine:pagnationLink linkType="prev"/>">Prev</a>
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="prev" includeIfTrue="false">
                                Prev
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="next" includeIfTrue="true">
                                <a href="/quarantine<quarantine:pagnationLink linkType="next"/>">|Next</a>
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="next" includeIfTrue="false">
                                |Next
                              </quarantine:hasPagnation>
                            </div>
                          </td>
<!--                        
                          <th scope="row">Total</th>
                          <td colspan="4"><quarantine:indexMsgCount/> mails</td>
-->                          
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
                            <td>
                              <quarantine:inboxRecord prop="from"/>
                              <quarantine:hasSafelist includeIfTrue="true">
                                <a href="/quarantine/imc?<quarantine:constants keyName="action"/>=<quarantine:constants valueName="sladd"/>&<quarantine:constants keyName="tkn"/>=<quarantine:currentAuthToken encoded="false"/>&<quarantine:constants keyName="sort"/>=<quarantine:pagnationProperties propName="sorting"/>&<quarantine:constants keyName="ascend"/>=<quarantine:pagnationProperties propName="ascending"/>&<quarantine:constants keyName="first"/>=<quarantine:pagnationProperties propName="thisId"/>&<quarantine:constants keyName="sladdr"/>=<quarantine:inboxRecord prop="from"/>">(Safelist)</a>
                              </quarantine:hasSafelist>
                            </td>
                            <td><quarantine:inboxRecord prop="detail"/></td>
                            <td><quarantine:inboxRecord prop="subject"/></td>
                          </tr>
                        </quarantine:forEachInboxRecord>
                      </tbody>
                    </table>
                  </td>
                </tr>
              </tbody>
            </table>
          </form>
          </quarantine:hasInboxRecords>
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