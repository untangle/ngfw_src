/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.DayOfWeekMatcher;

/**
 * System settings.
 */
@SuppressWarnings("serial")
public class SystemSettings implements Serializable, JSONString
{
    private int version = 3;
    private int httpsPort;

    private boolean supportEnabled = false;
    private boolean cloudEnabled = true;
    private boolean httpAdministrationAllowed = true;
    private String administrationSubnets = null;

    private boolean autoUpgrade;
    private DayOfWeekMatcher autoUpgradeDays;
    private int autoUpgradeHour   = 2;
    private int autoUpgradeMinute = 0;

    private SnmpSettings snmpSettings;

    private String timeSource = "ntp";
    
    /**
     * These are used to indicate which certificate is assigned to each of
     * the services that are provided by this server. We assign apache.pem
     * to each by default, since that is the name of the cert that is
     * created and signed by our internal CA during the installation.
     */
    private String webCertificate = "apache.pem";
    private String mailCertificate = "apache.pem";
    private String ipsecCertificate = "apache.pem";
    private String radiusCertificate = "apache.pem";

    private boolean radiusServerEnabled = false;
    private String radiusServerSecret = "SharedSecret";

    private String installType = "";

    public SystemSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Get whether or not local insecure administration is enabled.
     */
    public boolean getHttpAdministrationAllowed() { return this.httpAdministrationAllowed; }
    public void setHttpAdministrationAllowed( boolean newValue ) { this.httpAdministrationAllowed = newValue; }

    /**
     * Get whether or not local insecure administration is enabled.
     */
    public String getAdministrationSubnets() { return this.administrationSubnets; }
    public void setAdministrationSubnets( String newValue ) { this.administrationSubnets = newValue; }
    
    /**
     * Untangle cloud access flag
     */
    public boolean getCloudEnabled() { return this.cloudEnabled; }
    public void setCloudEnabled( boolean newValue ) { this.cloudEnabled = newValue; }

    /**
     * Untangle support access flag
     */
    public boolean getSupportEnabled() { return this.supportEnabled; }
    public void setSupportEnabled( boolean newValue ) { this.supportEnabled = newValue; }

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
    
    /**
    * Get time source
    */
    public String getTimeSource(){ return timeSource; }
    public void setTimeSource( String newValue) { this.timeSource = newValue; }

    /**
     * Install Type
     */
    public String getInstallType(){ return installType; }
    public void setInstallType( String newValue) { this.installType = newValue; }

    /**
     * These are used to get and set the certificates used by the
     * different SSL secured services provided by this server
     */
    public String getWebCertificate() { return webCertificate; }
    public String getMailCertificate() { return mailCertificate; }
    public String getIpsecCertificate() { return ipsecCertificate; }
    public String getRadiusCertificate() { return radiusCertificate; }
    public void setWebCertificate(String newValue) { this.webCertificate = newValue; }
    public void setMailCertificate(String newValue) { this.mailCertificate = newValue; }
    public void setIpsecCertificate(String newValue) { this.ipsecCertificate = newValue; }
    public void setRadiusCertificate(String newValue) { this.radiusCertificate = newValue; }

    /**
     * These are used to get and set the radius server flag and secret
     */
    public boolean getRadiusServerEnabled() { return radiusServerEnabled; }
    public void setRadiusServerEnabled(boolean newValue) { this.radiusServerEnabled = newValue; }
    public String getRadiusServerSecret() { return radiusServerSecret; }
    public void setRadiusServerSecret(String newValue) { this.radiusServerSecret = newValue; }

    /* DEPRECATED in 12.2 - moved to network settings */
    /* DEPRECATED in 12.1 - moved to network settings */
    /* DEPRECATED in 12.1 - moved to network settings */
    private String publicUrlMethod;
    private String publicUrlAddress;
    private int publicUrlPort = 443;
    public String deprecated_getPublicUrlMethod() { return this.publicUrlMethod; }
    public void setPublicUrlMethod( String newValue ) { this.publicUrlMethod = newValue; }
    public String deprecated_getPublicUrlAddress() { return this.publicUrlAddress; }
    public void setPublicUrlAddress( String newValue ) { this.publicUrlAddress = newValue; }
    public int deprecated_getPublicUrlPort() { return this.publicUrlPort; }
    public void setPublicUrlPort( int newValue ) { this.publicUrlPort = newValue; }
    /* DEPRECATED in 12.2 - moved to network settings */
    /* DEPRECATED in 12.1 - moved to network settings */
    /* DEPRECATED in 12.1 - moved to network settings */
}
