/**
 * $Id$
 */

package com.untangle.uvm;

import java.util.Date;
import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Class to store certificate details for display in the user interface
 */
@SuppressWarnings("serial")
public class CertificateInformation implements JSONString, Serializable
{
    private String fileName;
    private Date dateValid;
    private Date dateExpires;
    private String certSubject;
    private String certIssuer;
    private String certNames;
    private String certUsage;
    private boolean httpsServer;
    private boolean smtpsServer;
    private boolean ipsecServer;

// THIS IS FOR ECLIPSE - @formatter:off

    public String getFileName() { return fileName; }
    public void setFileName(String argValue) { fileName = argValue; }

    public Date getDateValid() { return dateValid; }
    public void setDateValid(Date argValue) { dateValid = argValue; }

    public Date getDateExpires() { return dateExpires; }
    public void setDateExpires(Date argValue) { dateExpires = argValue; }

    public String getCertSubject() { return certSubject; }
    public void setCertSubject(String argValue) { certSubject = argValue; }

    public String getCertIssuer() { return certIssuer; }
    public void setCertIssuer(String argValue) { certIssuer = argValue; }

    public String getCertNames() { return certNames; }
    public void setCertNames(String argValue) { certNames = argValue; }

    public String getCertUsage() { return certUsage; }
    public void setCertUsage(String argValue) { certUsage = argValue; }

    public boolean getHttpsServer() { return httpsServer; }
    public void setHttpsServer(boolean argValue) { httpsServer = argValue; }

    public void setSmtpsServer(boolean argValue) { smtpsServer = argValue; }
    public boolean getSmtpsServer() { return smtpsServer; }

    public void setIpsecServer(boolean argValue) { ipsecServer = argValue; }
    public boolean getIpsecServer() { return ipsecServer; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

// THIS IS FOR ECLIPSE - @formatter:off
    
}
