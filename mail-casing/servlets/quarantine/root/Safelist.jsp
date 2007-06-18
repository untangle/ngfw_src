<%@page language="java" import="com.untangle.uvm.*"%>
        
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
LocalUvmContext uvm = LocalUvmContextFactory.context();
BrandingSettings bs = uvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName(); 
String companyUrl = bs.getCompanyUrl(); 
%>      
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | Safelist for <quarantine:currentAddress/></title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
 <link rel="stylesheet" href="styles/style.css" type="text/css" />
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
<script type="text/javascript">
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
<div id="main">
 <!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->
	
	<div class="page_head">
        <a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" border="0" alt="<%=company%> logo"/></a> <div>Safelist for:<br /><quarantine:currentAddress/></div>
	</div>
	
    <hr />  
  
        <center>
        <div style="padding: 10px 0; margin: 0 auto; width: 440px;">
        
        Welcome to your &quot;safelist.&quot;<br /><br />
            
        A safelist is a list of email addresses, usually email addresses of your friends and colleagues. Emails, sent by anyone on a safelist, will <i>not</i> be quarantined even if the emails were to be identified as spam or phish.<br /><br />
        
        To delete any email address on the safelist, click the checkboxes for one or more email addresses and click <code>Remove Selected Addresses</code>. To add an email address to the safelist, enter the email address in the text field and click <code>Submit</code>.<br /><br />
        
        </div>
        </center>

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
          <form name="form1" method="POST" action="safelist">
            <input type="hidden"
              name="<quarantine:constants keyName="action"/>"
              value="<quarantine:constants valueName="slremove"/>" />
            <input type="hidden"
              name="<quarantine:constants keyName="tkn"/>"
              value="<quarantine:currentAuthToken encoded="false"/>" />
            <table>
              <tr>
                <td>
                  <table class="slist" width="100%">
                    <thead>
                      <tr>
                        <th><input type="checkbox"
                          name="checkall"
                          value="checkall"
                          onclick="CheckAll()" /></th>
                        <th width="100%">Email Address</th>
                      </tr>
                   </thead>
                    <tbody>
                      <quarantine:forEachSafelistEntry>
                        <tr>
                          <td><input type="checkbox"
                            name="<quarantine:constants keyName="sladdr"/>"
                            value="<quarantine:safelistEntry encoded="false"/>" /></td>
                          <td><quarantine:safelistEntry encoded="false"/></td>
                        </tr>
                      </quarantine:forEachSafelistEntry>
                    </tbody>
                    <tfoot>
                      <tr>
                        <td colspan="2"><input type="Submit" value="Remove Selected Addresses" /></td>
                      </tr>
                    </tfoot>
                  </table>
                </td>
              </tr>
            </table>
          </form>

      <!-- INPUT FORM 2 -->
          <br /><br />
          <form name="form2" action="safelist">
            <input type="hidden"
              name="<quarantine:constants keyName="action"/>"
              value="<quarantine:constants valueName="sladd"/>" />
            <input type="hidden"
              name="<quarantine:constants keyName="tkn"/>"
              value="<quarantine:currentAuthToken encoded="false"/>" />
            <table>
              <tr>
                <td class="enteremail">
                  <table>
                    <tr>
                      <td>Add address: </td>
                      <td><input type="text" name="<quarantine:constants keyName="sladdr"/>" />
                    </tr>
                    <tr><td colspan="2"><button type="submit">Submit</button></td></tr>
                  </table>
                </td>
              </tr>
            </table>
          </form>


        <br/>
    <address>Powered by Untangle&trade; Server</address>

	<hr />
	
 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>	

</body>
</html>

