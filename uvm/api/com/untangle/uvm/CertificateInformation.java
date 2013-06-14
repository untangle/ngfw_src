/*
 * $Id: CertificateInformation.java 0 2013-03-04 20:00:00Z mahotz $
*/

package com.untangle.uvm;

import java.util.Date;
import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;

@SuppressWarnings("serial")
public class CertificateInformation implements JSONString, Serializable
{
    private Date rootcaDateValid;
    private Date rootcaDateExpires;
    private String rootcaSubject;

    private Date serverDateValid;
    private Date serverDateExpires;
    private String serverSubject;
    private String serverIssuer;

    public void setRootcaDateValid(Date argValue) { rootcaDateValid = argValue; }
    public void setRootcaDateExpires(Date argValue) { rootcaDateExpires = argValue; }
    public void setRootcaSubject(String argValue) { rootcaSubject = argValue; }

    public void setServerDateValid(Date argValue) { serverDateValid = argValue; }
    public void setServerDateExpires(Date argValue) { serverDateExpires = argValue; }
    public void setServerSubject(String argValue) { serverSubject = argValue; }
    public void setServerIssuer(String argValue) { serverIssuer = argValue; }

    public Date getRootcaDateValid() { return rootcaDateValid; }
    public Date getRootcaDateExpires() { return rootcaDateExpires; }
    public String getRootcaSubject() { return rootcaSubject; }

    public Date getServerDateValid() { return serverDateValid; }
    public Date getServerDateExpires() { return serverDateExpires; }
    public String getServerSubject() { return serverSubject; }
    public String getServerIssuer() { return serverIssuer; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
