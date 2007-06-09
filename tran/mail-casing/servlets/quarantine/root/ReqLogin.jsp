<%@page language="java" import="com.untangle.uvm.*"%>

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
UvmLocalContext uvm = UvmContextFactory.context();
BrandingSettings bs = uvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();
String companyUrl = bs.getCompanyUrl();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | Request Quarantine Digest Email</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
</head>
<body>
<div id="main">    
<!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->
	
	<div class="page_head">
		<a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" border="0" alt="<%=company%>" /></a> 
		<div>Request Quarantine Digest Email</div>
	</div>

    <hr />
	
	<center>
	<div style="padding: 10px 0; margin: 0 auto; width: 80%;">
	
        <!-- INTRO MESSAGE -->
        You can request that an email message, of your quarantined emails, be sent to your email account.
        <br /><br />
        Please enter your email address into the form below and click <code>Submit</code>.
        You will receive an email message containing a list of your recent quarantined emails and a link to your quarantined email page.
        When you receive this email message, you can release or delete your quarantined emails from this list or access your quarantined email page from your browser to release or delete your quarantined emails.
        <br />

        <!-- MAIN MESSAGE -->
        <br />
	</div>
    </center>	
        <center>
              <quarantine:hasMessages type="info">
                  <ul class="messageText">
                    <quarantine:forEachMessage type="info">
                      <li><quarantine:message/></li>
                    </quarantine:forEachMessage>
                  </ul>
              </quarantine:hasMessages>
			<br /><br />
              <quarantine:hasMessages type="error">
                  <ul class="errortext">
                    <quarantine:forEachMessage type="error">
                      <li><quarantine:message/></li>
                    </quarantine:forEachMessage>
                  </ul>
              </quarantine:hasMessages>
        </center>
        <center>		
            <form name="form1" method="POST" action="requestdigest">
                  <table style="border: 1px solid; padding: 10px">
                    <tr>
                      <td>Email Address:&nbsp;</td>
                      <td>
                        <input type="text" name="<quarantine:constants keyName="draddr"/>" />
                      </td>
                    </tr>
                    <tr>
                      <td class="paddedButton" colspan="2" align="center">
                        <input type="submit" value="Submit" />
                      </td>
                    </tr>
                  </table>
            </form>
        </center>
        <br />
		
    <center>Powered by Untangle&trade; Server</center>


	<hr />

 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>	
</body>
</html>
