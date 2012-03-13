/**
 * $Id$
 */
package com.untangle.node.virus;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.GenericRule;

/**
 * Settings for the Virus Blockers
 */
@SuppressWarnings("serial")
public class VirusSettings implements Serializable
{
    private boolean allowFtpResume = true;
    private boolean allowHttpResume = true;

    private boolean scanHttp = true;
    private boolean scanFtp = true;
    private boolean scanSmtp = true;
    private boolean scanPop = true;
    private boolean scanImap = true;

    private String smtpAction= "remove" ; /* "pass" "remove" or "block" */
    private String popAction = "remove" ;  /* "pass" "remove" */
    private String imapAction = "remove" ; /* "pass" "remove" */
    
    private List<GenericRule> httpMimeTypes = new LinkedList<GenericRule>();
    private List<GenericRule> httpFileExtensions = new LinkedList<GenericRule>();

    // constructors -----------------------------------------------------------

    public VirusSettings() { }

    // accessors --------------------------------------------------------------

    public boolean getScanHttp() { return scanHttp; }
    public void setScanHttp(boolean scanHttp) { this.scanHttp = scanHttp; }

    public boolean getScanFtp() { return scanFtp; }
    public void setScanFtp(boolean scanFtp) { this.scanFtp = scanFtp; }

    public boolean getScanSmtp() { return scanSmtp; }
    public void setScanSmtp(boolean scanSmtp) { this.scanSmtp = scanSmtp; }

    public boolean getScanPop() { return scanPop; }
    public void setScanPop(boolean scanPop) { this.scanPop = scanPop; }

    public boolean getScanImap() { return scanImap; }
    public void setScanImap(boolean scanImap) { this.scanImap = scanImap; }

    public String getSmtpAction() { return smtpAction; }
    public void setSmtpAction(String smtpAction) { this.smtpAction = smtpAction; }

    public String getPopAction() { return popAction; }
    public void setPopAction(String popAction) { this.popAction = popAction; }

    public String getImapAction() { return imapAction; }
    public void setImapAction(String imapAction) { this.imapAction = imapAction; }
    
    public List<GenericRule> getHttpMimeTypes() { return httpMimeTypes; }
    public void setHttpMimeTypes(List<GenericRule> httpMimeTypes) { this.httpMimeTypes = httpMimeTypes; }

    public List<GenericRule> getHttpFileExtensions() { return httpFileExtensions; }
    public void setHttpFileExtensions(List<GenericRule> httpFileExtensions) { this.httpFileExtensions = httpFileExtensions; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
