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
    private boolean supportEnabled;
    private boolean insideHttpEnabled;
    private boolean outsideHttpsEnabled;
    private boolean outsideHttpsReportingEnabled;
    private boolean outsideHttpsAdministrationEnabled;
    private boolean outsideHttpsQuarantineEnabled;
    private int httpsPort;

    public static final String PUBLIC_URL_EXTERNAL_IP = "external";
    public static final String PUBLIC_URL_HOSTNAME = "hostname";
    public static final String PUBLIC_URL_ADDRESS_AND_PORT = "address_and_port";
    
    private String publicUrlMethod;
    private String publicUrlAddress;
    private int publicUrlPort;

    private SnmpSettings snmpSettings;
    
    public SystemSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * untangle support access flag
     */
    public boolean getSupportEnabled() { return this.supportEnabled; }
    public void setSupportEnabled( boolean newValue ) { this.supportEnabled = newValue; }

    /**
     * This is the port that the HTTPS server lives on
     */
    public int getHttpsPort() { return this.httpsPort; }
    public void setHttpsPort( int newValue ) { this.httpsPort = newValue ; }

    /**
     * Get whether or not local insecure access is enabled.
     */
    public boolean getInsideHttpEnabled() { return this.insideHttpEnabled; }
    public void setInsideHttpEnabled( boolean newValue ) { this.insideHttpEnabled = newValue; }

    /**
     * Retrieve whether or not administration from the internet is allowed.
     */
    public boolean getOutsideHttpsEnabled() { return this.outsideHttpsEnabled; }
    public void setOutsideHttpsEnabled( boolean newValue ) { this.outsideHttpsEnabled = newValue; }

    /**
     * Retrieve whether access is allowed to reports from the internet.
     */
    public boolean getOutsideHttpsReportingEnabled() { return this.outsideHttpsReportingEnabled; }
    public void setOutsideHttpsReportingEnabled( boolean newValue ) { this.outsideHttpsReportingEnabled = newValue; }

    /**
     * Get whether or not external administration is allowed.
     */
    public boolean getOutsideHttpsAdministrationEnabled() { return this.outsideHttpsAdministrationEnabled; }
    public void setOutsideHttpsAdministrationEnabled( boolean newValue ) { this.outsideHttpsAdministrationEnabled = newValue; }

    /**
     * Retrieve whether or not to access the user quarantine from the
     * internet is allowed.
     */
    public boolean getOutsideHttpsQuarantineEnabled() { return this.outsideHttpsQuarantineEnabled; }
    public void setOutsideHttpsQuarantineEnabled( boolean newValue ) { this.outsideHttpsQuarantineEnabled = newValue; }

    /**
     * This determines the method used to calculate the publicy available URL used to reach Untangle resources
     */
    public String getPublicUrlMethod() { return this.publicUrlMethod; }
    public void setPublicUrlMethod( String newValue ) { this.publicUrlMethod = newValue; }

    /**
     * This stores the hostname/IP used to reach Untangle publicly (if specified)
     */
    public String getPublicUrlAddress() { return this.publicUrlAddress; }
    public void setPublicUrlAddress( String newValue ) { this.publicUrlAddress = newValue; }

    /**
     * This stores the port used to reach Untangle publicly (if specified)
     */
    public int getPublicUrlPort() { return this.publicUrlPort; }
    public void setPublicUrlPort( int newValue ) { this.publicUrlPort = newValue; }

    /**
     * The SMNP settings
     */
    public SnmpSettings getSnmpSettings() { return this.snmpSettings; }
    public void setSnmpSettings( SnmpSettings newValue ) { this.snmpSettings = newValue; }
    
}
