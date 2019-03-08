/**
 * $Id: TrustedCertificate.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.ssl_inspector;

import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * This class is used to store the details of a trusted certificate that has
 * been uploaded by the user.
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class TrustedCertificate implements JSONString, Serializable
{
    private String certAlias = null;
    private String issuedTo = null;
    private String issuedBy = null;
    private String dateValid = null;
    private String dateExpire = null;

    public void setCertAlias(String certAlias)
    {
        this.certAlias = certAlias;
    }

    public void setIssuedTo(String issuedTo)
    {
        this.issuedTo = issuedTo;
    }

    public void setIssuedBy(String issuedBy)
    {
        this.issuedBy = issuedBy;
    }

    public void setDateValid(String dateValid)
    {
        this.dateValid = dateValid;
    }

    public void setDateExpire(String dateExpire)
    {
        this.dateExpire = dateExpire;
    }

    public String getCertAlias()
    {
        return certAlias;
    }

    public String getIssuedTo()
    {
        return issuedTo;
    }

    public String getIssuedBy()
    {
        return issuedBy;
    }

    public String getDateValid()
    {
        return dateValid;
    }

    public String getDateExpire()
    {
        return dateExpire;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
