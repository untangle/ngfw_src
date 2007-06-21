<%@page language="java" import="com.untangle.uvm.*"%>
<%@ taglib uri="/WEB-INF/taglibs/quarantine_euv.tld" prefix="quarantine" %>
<%--
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--%>

<%
LocalUvmContext uvm = LocalUvmContextFactory.context();
BrandingSettings bs = uvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();
String companyUrl = bs.getCompanyUrl();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | Aliases forwarding to <quarantine:currentAddress/></title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" href="styles/style.css" type="text/css"/>
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
<script type="text/javascript">
// <![CDATA[
function CheckAll() {
count = document.form1.elements.length;
isOn = document.form1.checkall.checked;
    for (i=0; i < count; i++) {
      document.form1.elements[i].checked = isOn;
    }
}
// ]]>
</script>
</head>
<body>
<div id="main">
 <!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->
 
 	<div class="page_head">
        <a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" border="0" alt="<%=company%> logo"/></a> <div>Aliases forwarding to:<br/><quarantine:currentAddress/></div>
	</div>
    
	<hr />

        <!-- INTRO MESSAGE -->
	    <center>
    	<div style="padding: 10px 0; margin: 0 auto; width: 440px; text-align:left;">        
        This is a list of email addresses that have their quarantined emails
        forwarded to the quarantine of &quot;<quarantine:currentAddress/>&quot;.
        To stop forwarding quarantined emails for one or more of these addresses,
        click the checkboxes for one or more addresses and click <code>Remove Selected Addresses</code>.
        </div>
        </center>

        <!-- INITIAL TABLE -->
        <center>
        <table><tbody>
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
        </tbody></table>
        <table><tbody>
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
        </tbody></table>
        </center>

        <!-- INPUT FORM 1 -->
          <form name="form1" method="POST" action="unmp">
            <input type="hidden"
              name="<quarantine:constants keyName="action"/>"
              value="unmapremove"/>
            <input type="hidden"
              name="<quarantine:constants keyName="tkn"/>"
              value="<quarantine:currentAuthToken encoded="false"/>"/>
            <table class="slist" width="100%">
                <thead>
                  <tr>
                    <th><input type="checkbox" name="checkall" value="checkall" onclick="CheckAll()" /></th>
                    <th>Email Address</th>
                  </tr>
                </thead>
                <tbody>
                  <quarantine:forEachReceivingRemapsEntry>
                    <tr>
                      <td><input type="checkbox" name="unmapaddr" value="<quarantine:receivingRemapsEntry encoded="false"/>" /></td>
                      <td><quarantine:receivingRemapsEntry encoded="false" /></td>
                    </tr>
                  </quarantine:forEachReceivingRemapsEntry>
                </tbody>
                <tfoot>
                  <tr><td colspan="2"><button type="submit">Remove Selected Addresses</button></td></tr>
                </tfoot>
              </table>
          </form>
        <br />
        <address>Powered by Untangle&trade; Server</address>


	<hr />

	
 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>
</body>
</html> 
