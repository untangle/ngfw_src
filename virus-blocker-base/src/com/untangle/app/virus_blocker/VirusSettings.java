/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.GenericRule;

/**
 * Settings for the Virus Blockers
 */
@SuppressWarnings("serial")
public class VirusSettings implements Serializable, JSONString
{
    private boolean allowFtpResume = true;
    private boolean allowHttpResume = true;

    private boolean scanHttp = true;
    private boolean scanFtp = true;
    private boolean scanSmtp = true;

    private boolean enableCloudScan = true;
    private boolean enableLocalScan = true;

    private String  smtpAction= "remove" ; /* "pass" "remove" or "block" */
    private boolean smtpAllowTls = true;

    private Boolean customBlockPageEnabled = false;
    private String customBlockPageUrl = "";

    private List<GenericRule> httpMimeTypes = new LinkedList<GenericRule>();
    private List<GenericRule> httpFileExtensions = new LinkedList<GenericRule>();
    private List<GenericRule> passSites = new LinkedList<GenericRule>();

    /*
     * On systems where we can't write to temp files we operate in a mode
     * where we calculate the MD5 and only use the cloud scanner.  This
     * flag allows that mode to manually be enabled and tested.
     */
    private boolean forceMemoryMode = false;

    public VirusSettings() { }

    public boolean getScanHttp() { return scanHttp; }
    public void setScanHttp(boolean scanHttp) { this.scanHttp = scanHttp; }

    public boolean getScanFtp() { return scanFtp; }
    public void setScanFtp(boolean scanFtp) { this.scanFtp = scanFtp; }

    public boolean getScanSmtp() { return scanSmtp; }
    public void setScanSmtp(boolean scanSmtp) { this.scanSmtp = scanSmtp; }

    public boolean getEnableCloudScan() { return enableCloudScan; }
    public void setEnableCloudScan(boolean scanSmtp) { this.enableCloudScan = scanSmtp; }

    public boolean getEnableLocalScan() { return enableLocalScan; }
    public void setEnableLocalScan(boolean scanSmtp) { this.enableLocalScan = scanSmtp; }

    public String getSmtpAction() { return smtpAction; }
    public void setSmtpAction(String smtpAction) { this.smtpAction = smtpAction; }

    public boolean getSmtpAllowTls() { return smtpAllowTls; }
    public void setSmtpAllowTls( boolean newValue ) { this.smtpAllowTls = newValue; }

    public List<GenericRule> getHttpMimeTypes() { return httpMimeTypes; }
    public void setHttpMimeTypes(List<GenericRule> httpMimeTypes) { this.httpMimeTypes = httpMimeTypes; }

    public List<GenericRule> getPassSites() { return passSites; }
    public void setPassSites(List<GenericRule> passSites) { this.passSites = passSites; }

    public List<GenericRule> getHttpFileExtensions() { return httpFileExtensions; }
    public void setHttpFileExtensions(List<GenericRule> httpFileExtensions) { this.httpFileExtensions = httpFileExtensions; }

    public boolean getForceMemoryMode() { return forceMemoryMode; }
    public void setForceMemoryMode(boolean newValue ) { this.forceMemoryMode = newValue; }

    public Boolean getCustomBlockPageEnabled() { return customBlockPageEnabled; }
    public void setCustomBlockPageEnabled(Boolean customBlockPageEnabled) { this.customBlockPageEnabled = customBlockPageEnabled; }

    public String getCustomBlockPageUrl() { return this.customBlockPageUrl; }
    public void setCustomBlockPageUrl(String customBlockPageUrl) { this.customBlockPageUrl = customBlockPageUrl; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
