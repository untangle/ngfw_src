/**
 * $Id: SystemSettings.java 31993 2012-05-22 17:59:09Z dmorris $
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * System settings.
 */
@SuppressWarnings("serial")
public class SystemSettings implements Serializable, JSONString
{
    private boolean isSupportEnabled;
    private boolean isInsideInsecureEnabled;
    private boolean isOutsideHttpsEnabled;
    private boolean isOutsideReportingEnabled;
    private boolean isOutsideAdministrationEnabled;
    private boolean isOutsideQuarantineEnabled;
    private int httpsPort;
    
    public SystemSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * untangle support access flag
     */
    public boolean getIsSupportEnabled() { return this.isSupportEnabled; }
    public void setIsSupportEnabled( boolean newValue ) { this.isSupportEnabled = newValue; }

    /**
     * The block page port (currently unsettable)
     */
    public int getBlockPagePort() { return 80; }
    public void setBlockPagePort( int port ) { return; }

    /**
     * This is the port that the HTTPS server lives on
     */
    public int getHttpsPort() { return this.httpsPort; }
    public void setHttpsPort( int newValue ) { this.httpsPort = newValue ; }

    /**
     * Get whether or not local insecure access is enabled.
     */
    public boolean getIsInsideInsecureEnabled() { return this.isInsideInsecureEnabled; }
    public void setIsInsideInsecureEnabled( boolean newValue ) { this.isInsideInsecureEnabled = newValue; }

    /**
     * Retrieve whether or not administration from the internet is allowed.
     */
    public boolean getIsOutsideHttpsEnabled() { return this.isOutsideHttpsEnabled; }
    public void setIsOutsideHttpsEnabled( boolean newValue ) { this.isOutsideHttpsEnabled = newValue; }

    /**
     * Retrieve whether access is allowed to reports from the internet.
     */
    public boolean getIsOutsideReportingEnabled() { return this.isOutsideReportingEnabled; }
    public void setIsOutsideReportingEnabled( boolean newValue ) { this.isOutsideReportingEnabled = newValue; }

    /**
     * Get whether or not external administration is allowed.
     */
    public boolean getIsOutsideAdministrationEnabled() { return this.isOutsideAdministrationEnabled; }
    public void setIsOutsideAdministrationEnabled( boolean newValue ) { this.isOutsideAdministrationEnabled = newValue; }

    /**
     * Retrieve whether or not to access the user quarantine from the
     * internet is allowed.
     */
    public boolean getIsOutsideQuarantineEnabled() { return this.isOutsideQuarantineEnabled; }
    public void setIsOutsideQuarantineEnabled( boolean newValue ) { this.isOutsideQuarantineEnabled = newValue; }
}
