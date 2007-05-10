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

MvvmLocalContext mvvm = MvvmContextFactory.context();
BrandingSettings bs = mvvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();
String companyUrl = bs.getCompanyUrl();
%>
<%@ taglib uri="/WEB-INF/taglibs/quarantine_euv.tld" prefix="quarantine" %>


<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<!-- HEADING -->
  <title><%=company%> | Quarantine Digest for <quarantine:currentAddress/></title>
    <script>
      function CheckAll() {
        count = document.form1.elements.length;
        isOn = document.form1.checkall.checked;
        for (i=0; i < count; i++) {
          document.form1.elements[i].checked = isOn;
        }
      }

      function uncheckMaster() {
        document.form1.checkall.checked = false;
      }

      function doRefresh() {
        document.form1.<quarantine:constants keyName="action"/>.value = "<quarantine:constants valueName="refresh"/>";
        document.form1.submit();
      }

      function doPurge() {
        document.form1.<quarantine:constants keyName="action"/>.value = "<quarantine:constants valueName="purge"/>";
        document.form1.submit();
      }

      function doRescue() {
        document.form1.<quarantine:constants keyName="action"/>.value = "<quarantine:constants valueName="rescue"/>";
        document.form1.submit();
      }

      function doResort(newSort) {
        var nextAscend = "true"
        if(document.form1.<quarantine:constants keyName="sort"/>.value == newSort) {
          //If not changing the sort criteria, the toggle up/down
          if(document.form1.<quarantine:constants keyName="ascend"/>.value == "true") {
            nextAscend = "false";
          }
        }
        else {
          //We're switching to a new criteria, so use ascending
          //by default
          document.form1.<quarantine:constants keyName="sort"/>.value = newSort;
          nextAscend = "true";
        }
        document.form1.<quarantine:constants keyName="ascend"/>.value = nextAscend;
        document.form1.submit();
      }

      function doResortByInternDate() {
        doResort("0");
      }

      function doResortBySize() {
        doResort("1");
      }

      function doResortBySender() {
        doResort("2");
      }

      function doResortBySubject() {
        doResort("3");
      }

      function doResortByScore() {
        doResort("4");
      }

      function doResortByAttachmentCount() {
        doResort("5");
      }

      function mv_doNext() {
        document.form1.<quarantine:constants keyName="first"/>.value = <quarantine:pagnationProperties propName="nextId"/>;
        document.form1.submit();
      }

      function mv_doPrev() {
        document.form1.<quarantine:constants keyName="first"/>.value = <quarantine:pagnationProperties propName="prevId"/>;
        document.form1.submit();
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
        Quarantine Digest for:<br/><quarantine:currentAddress/>
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
      <quarantine:isRemapped includeIfTrue="true">
        Quarantined emails for &quot;<quarantine:currentAddress/>&quot;
        have been forwarded to the quarantine of &quot;<quarantine:remappedTo encoded="false"/>&quot;.
        If you believe this to be an error,
        please contact either your system administrator or
        <a href="mailto:<quarantine:remappedTo encoded="false"/>"><quarantine:remappedTo encoded="false"/></a> directly.
        <br><br>
      </quarantine:isRemapped>

      <quarantine:isRemapped includeIfTrue="false">
        <quarantine:hasInboxRecords includeIfTrue="true">
          The emails listed below have been quarantined by the <%=company%> Server and
          will be deleted automatically after <quarantine:maxDaysToIntern/> days.
          <br><br>
          To release emails from the quarantine and
          have them delivered to your email account or
          to delete emails from the quarantine,
          click the checkboxes for one or more emails and
          click <code>Release</code> or <code>Delete</code>, respectively.
          <br><br>
        </quarantine:hasInboxRecords>

        <quarantine:hasInboxRecords includeIfTrue="false">
          You have no quarantined emails.
          <br><br>
        </quarantine:hasInboxRecords>

        <quarantine:hasSafelist includeIfTrue="true">
          You may also view your <a href="/quarantine/safelist?<quarantine:constants keyName="action"/>=<quarantine:constants valueName="slview"/>&<quarantine:constants keyName="tkn"/>=<quarantine:currentAuthToken encoded="true"/>">safelist</a>
          of email senders whose emails you never want to quarantine.
          <br><br>
        </quarantine:hasSafelist>

        <quarantine:isReceivesRemaps includeIfTrue="true">
          <!--
            This address received remaps.  Do not offer to let them pass
            their account along until all inbound remaps are empty
          -->
          Note that quarantined emails for &quot;<quarantine:currentAddress/>&quot;
          as well as for:
          <ul>
            <quarantine:forEachReceivingRemapsEntry>
              <li><quarantine:receivingRemapsEntry encoded="false"/></li>
            </quarantine:forEachReceivingRemapsEntry>
          </ul>
          are forwarded to this quarantine.
          If you wish to change the email addresses that are associated with
          this address, please visit
          <a href="/quarantine/unmp?action=unmapview&tkn=<quarantine:currentAuthToken encoded="true"/>">the alias control</a> page.
          <br><br>
        </quarantine:isReceivesRemaps>

        <quarantine:isReceivesRemaps includeIfTrue="false">
          <!-- Offer to remap -->
          You can also forward all quarantined email for
          &quot;<quarantine:currentAddress/>&quot; to the quarantine of a different email address.
          For example,
          when you have a mailing list (such as &quot;sales@mycompany.com&quot; or &quot;jobs@anotherCompany.com&quot;),
          you can have a single person manage the quarantined emails for this list.
          To enable forwarding, please visit
          <a href="/quarantine/mp?action=mapview&tkn=<quarantine:currentAuthToken encoded="true"/>">the quarantine redirect</a> page.
          <br><br>
        </quarantine:isReceivesRemaps>
      </quarantine:isRemapped>

        <!-- MAIN MESSAGE -->
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

        <!-- MAIN TABLE -->
        <center>
          <quarantine:hasInboxRecords includeIfTrue="true">
          <form name="form1" method="POST" action="manageuser">
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
                    <table class="actions" border="0" cellpadding="0" cellspacing="0" width="100%">
                      <tbody>
                        <tr>
                          <td>
                            <input name="Release" value="Release" onclick="doRescue()" type="button">
                            <input name="Delete" value="Delete" onclick="doPurge()" type="button">
                          </td>
                          <td>
                            <div class="msiehack1">
                              <quarantine:hasPagnation linkType="prev" includeIfTrue="true">
                                <a href="javascript:mv_doPrev();">Prev</a>
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="prev" includeIfTrue="false">Prev</quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="next" includeIfTrue="true">
                                <a href="javascript:mv_doNext();">|Next</a>
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="next" includeIfTrue="false">|Next</quarantine:hasPagnation>
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
                          <th scope="col"><a href="javascript:doResortBySender();">From</a></th>
                          <th scope="col"><a href="javascript:doResortByAttachmentCount();">
                            <img src="images/with_attach.png" height="16px" width="16px"/></a></th>
                          <th scope="col"><a href="javascript:doResortByScore();">Score</a></th>
                          <th scope="col"><a href="javascript:doResortBySubject();">Subject</a></th>
                          <th scope="col"><a href="javascript:doResortByInternDate();">Date</a></th>
                          <th scope="col"><a href="javascript:doResortBySize();">Size (KB)</a></th>
                        </tr>
                      </thead>
                      <tfoot>
                        <tr>
                          <td colspan="3">
                            Mails Per Page: &nbsp;
                            <select name="rowsperpage" onChange="doRefresh();">
                            <quarantine:forEachRPPOption>
                              <quarantine:isRPPOptionSelected includeIfTrue="true">
                                <option value="<quarantine:rPPOption/>" selected="selected"><quarantine:rPPOption/></option>
                              </quarantine:isRPPOptionSelected>
                              <quarantine:isRPPOptionSelected includeIfTrue="false">
                                <option value="<quarantine:rPPOption/>"><quarantine:rPPOption/></option>
                              </quarantine:isRPPOptionSelected>
                            </quarantine:forEachRPPOption>
                            </select>
                          </td>

                          <th scope="row" align=left>TOTALS</th>
                          <td align=left><quarantine:indexNumRecords/><br/><quarantine:indexSizeRecords/></td>
                          <td> </td> <!-- table cell filler to shift navigation table cell to right (Prev|Next) -->

                          <td align=center width=130> <!-- align center w/ 130 pixel width to match navigation table cell in header -->
                            <div class="tableFooter">
                              <quarantine:hasPagnation linkType="prev" includeIfTrue="true">
                                <a href="javascript:mv_doPrev();">Prev</a>
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="prev" includeIfTrue="false">
                                Prev
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="next" includeIfTrue="true">
                                <a href="javascript:mv_doNext();">|Next</a>
                              </quarantine:hasPagnation>
                              <quarantine:hasPagnation linkType="next" includeIfTrue="false">
                                |Next
                              </quarantine:hasPagnation>
                            </div>
                          </td>
                        </tr>
                      </tfoot>
                      <tbody>
                        <quarantine:forEachInboxRecord>
                          <tr class="<quarantine:oddEven even="even" odd="odd"/>">
                            <th scope="row">
                              <input type="checkbox"
                                name="<quarantine:constants keyName="mailid"/>"
                                onclick="uncheckMaster()"
                                value="<quarantine:inboxRecord prop="mailid"/>"/>
                            </th>
                            <td>
                              <quarantine:inboxRecord prop="tsender" JSEscape="true"/>
                              <quarantine:hasSafelist includeIfTrue="true">
                                <a href="/quarantine/manageuser?<quarantine:constants keyName="action"/>=<quarantine:constants valueName="sladd"/>&<quarantine:constants keyName="tkn"/>=<quarantine:currentAuthToken encoded="true"/>&<quarantine:constants keyName="sort"/>=<quarantine:pagnationProperties propName="sorting"/>&<quarantine:constants keyName="ascend"/>=<quarantine:pagnationProperties propName="ascending"/>&<quarantine:constants keyName="first"/>=<quarantine:pagnationProperties propName="thisId"/>&<quarantine:constants keyName="rowsperpage"/>=<quarantine:pagnationProperties propName="rPPOption"/>&<quarantine:constants keyName="sladdr"/>=<quarantine:inboxRecord prop="sender"/>"><br/>(Safelist)</a>
                              </quarantine:hasSafelist>
                            </td>
                            <quarantine:hasAttachments includeIfTrue="true">
                              <td><img src="images/with_attach.png" height="16px" width="16px"/></td>
                            </quarantine:hasAttachments>
                            <quarantine:hasAttachments includeIfTrue="false">
                              <td><!--<img src="images/no_attach.png" height="16px" width="16px"/>--></td>
                            </quarantine:hasAttachments>
                            <td><quarantine:inboxRecord prop="fdetail" JSEscape="true"/></td>
                            <td><quarantine:inboxRecord prop="tsubject" JSEscape="true"/></td>
                            <td><quarantine:inboxRecord prop="fdate" JSEscape="true"/><br/><quarantine:inboxRecord prop="ftime" JSEscape="true"/></td>
                            <td><quarantine:inboxRecord prop="fsize" JSEscape="true"/></td>
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
        </center>

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
