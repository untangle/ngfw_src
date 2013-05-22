/**
 * $Id: SystemSettings.java 31993 2012-05-22 17:59:09Z dmorris $
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.DayOfWeekMatcher;

/**
 * System settings.
 */
@SuppressWarnings("serial")
public class SystemSettings implements Serializable, JSONString
{
    private int version = 1;
    private int httpsPort;

    public static final String PUBLIC_URL_EXTERNAL_IP = "external";
    public static final String PUBLIC_URL_HOSTNAME = "hostname";
    public static final String PUBLIC_URL_ADDRESS_AND_PORT = "address_and_port";
    
    private boolean supportEnabled;

    private String publicUrlMethod;
    private String publicUrlAddress;
    private int publicUrlPort;

    private boolean autoUpgrade;
    private DayOfWeekMatcher autoUpgradeDays;
    private int autoUpgradeHour   = 2;
    private int autoUpgradeMinute = 0;

    private SnmpSettings snmpSettings;
    
    public SystemSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Untangle support access flag
     */
    public boolean getSupportEnabled() { return this.supportEnabled; }
    public void setSupportEnabled( boolean newValue ) { this.supportEnabled = newValue; }

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
    
    /**
     * Specifies if apt-get upgrade should be run automatically after
     * an update.
     */
    public boolean getAutoUpgrade() { return autoUpgrade; }
    public void setAutoUpgrade( boolean newValue ) { this.autoUpgrade = newValue; }

    /**
     * What days to auto-upgrade
     */
    public DayOfWeekMatcher getAutoUpgradeDays() { return autoUpgradeDays; }
    public void setAutoUpgradeDays( DayOfWeekMatcher newValue) { this.autoUpgradeDays = newValue; }

    /**
     * What time to auto-upgrade
     */
    public int getAutoUpgradeHour() { return autoUpgradeHour; }
    public void setAutoUpgradeHour( int newValue) { this.autoUpgradeHour = newValue; }

    /**
     * What time to auto-upgrade
     */
    public int getAutoUpgradeMinute() { return autoUpgradeMinute; }
    public void setAutoUpgradeMinute( int newValue) { this.autoUpgradeMinute = newValue; }

    /**
     * Get the current settings version
     */
    public int getVersion() { return version; }
    public void setVersion( int newValue) { this.version = newValue; }
    

    
}
