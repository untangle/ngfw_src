/*
 * $Id$
 */

package com.untangle.uvm;

import java.util.Date;
import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;

@SuppressWarnings("serial")
public class CertificateInformation implements JSONString, Serializable
{
    private String fileName;
    private Date dateValid;
    private Date dateExpires;
    private String certSubject;
    private String certIssuer;
    private String certNames;
    private boolean httpsServer;
    private boolean smtpsServer;
    private boolean ipsecServer;

    public void setFileName(String argValue)
    {
        fileName = argValue;
    }

    public void setDateValid(Date argValue)
    {
        dateValid = argValue;
    }

    public void setDateExpires(Date argValue)
    {
        dateExpires = argValue;
    }

    public void setCertSubject(String argValue)
    {
        certSubject = argValue;
    }

    public void setCertIssuer(String argValue)
    {
        certIssuer = argValue;
    }

    public void setCertNames(String argValue)
    {
        certNames = argValue;
    }

    public void setHttpsServer(boolean argValue)
    {
        httpsServer = argValue;
    }

    public void setSmtpsServer(boolean argValue)
    {
        smtpsServer = argValue;
    }

    public void setIpsecServer(boolean argValue)
    {
        ipsecServer = argValue;
    }

    public String getFileName()
    {
        return fileName;
    }

    public Date getDateValid()
    {
        return dateValid;
    }

    public Date getDateExpires()
    {
        return dateExpires;
    }

    public String getCertSubject()
    {
        return certSubject;
    }

    public String getCertIssuer()
    {
        return certIssuer;
    }

    public String getCertNames()
    {
        return certNames;
    }

    public boolean getHttpsServer()
    {
        return httpsServer;
    }

    public boolean getSmtpsServer()
    {
        return smtpsServer;
    }

    public boolean getIpsecServer()
    {
        return ipsecServer;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
