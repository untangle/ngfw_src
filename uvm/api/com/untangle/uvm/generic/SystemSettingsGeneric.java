/**
 * $Id$
 */
package com.untangle.uvm.generic;

import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.LocaleInfo;
import com.untangle.uvm.SnmpSettings;
import com.untangle.uvm.SystemSettings;
import com.untangle.uvm.app.DayOfWeekMatcher;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.util.Constants;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Generic (v2) System Settings.
 */
@SuppressWarnings("serial")
public class SystemSettingsGeneric implements Serializable, JSONString {

    /**
     * Refers to {@link com.untangle.uvm.UvmContextImpl#isCCHidden()} .
     */
    private boolean isCCHidden;

    private int logRetention;

    /**
     * Refers to {@link com.untangle.uvm.SystemManagerImpl#getLogDirectorySize()} .
     */
    private Long logDirectorySize;

    /**
     * These are required for Web Admin Ports
     */
    private int httpPort  = 80;
    private int httpsPort = 443;

    /**
     * These are required for Hostname Settings
     */
    private String hostName;
    private String domainName;

    private boolean supportEnabled = false;
    private boolean cloudEnabled = true;
    private boolean httpAdministrationAllowed = true;
    private String administrationSubnets = null;

    private SnmpSettings snmpSettings;

    private boolean dynamicDnsServiceEnabled = false;
    private String  dynamicDnsServiceName = null;
    private String  dynamicDnsServiceUsername = null;
    private String  dynamicDnsServicePassword = null;
    private String  dynamicDnsServiceZone = null;
    private String  dynamicDnsServiceHostnames = null;
    private String  dynamicDnsServiceWan = "Default";

    private String  publicUrlMethod;
    private String  publicUrlAddress;
    private Integer publicUrlPort;
    private TimeZone timeZone = null;
    private Double thresholdTemperature = 105.0;

    private boolean enabled; // auto upgrade enabled
    private DayOfWeekMatcher autoUpgradeDays;
    private int hourOfDay   = 2;
    private int minuteOfHour = 0;

    /**
     * These are used for LocalDirectory RADIUS server support
     */
    private boolean radiusServerEnabled = false;
    private String radiusServerSecret = "SharedSecret";

    /**
     * These are used for RADIUS proxy support
     */
    private boolean radiusProxyEnabled = false;
    private String radiusProxyServer = StringUtils.EMPTY;
    private String radiusProxyWorkgroup = StringUtils.EMPTY;
    private String radiusProxyRealm = StringUtils.EMPTY;
    private String radiusProxyUsername = StringUtils.EMPTY;
    private String radiusProxyPassword = StringUtils.EMPTY;
    private String radiusProxyEncryptedPassword = StringUtils.EMPTY;

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

    private LanguageSettings languageSettings = null;
    private LinkedList<LocaleInfo> languagesList = null;

    public void setThresholdTemperature( Double newValue ) { this.thresholdTemperature = newValue; }
    public Double getThresholdTemperature() { return this.thresholdTemperature; }

    /**
     * Get log retention
     */
    public Integer getLogRetention(){ return this.logRetention; }
    public void setLogRetention( Integer newValue) { this.logRetention = newValue; }

    public Long getLogDirectorySize() {
        return logDirectorySize;
    }

    public void setLogDirectorySize(Long logDirectorySize) {
        this.logDirectorySize = logDirectorySize;
    }

    /**
     * These are required for TimeZone settings
     */
    public void setTimeZone(TimeZone timeZone) { this.timeZone = timeZone; }
    public TimeZone getTimeZone() { return timeZone; }

    public boolean isCCHidden() {
        return isCCHidden;
    }

    public void setCCHidden(boolean isCCHidden) {
        this.isCCHidden = isCCHidden;
    }

    /**
     * These are required for Web Admin Ports
     */
    public int getHttpPort() { return httpPort; }
    public void setHttpPort(int httpPort) { this.httpPort = httpPort; }
    public int getHttpsPort() { return httpsPort; }
    public void setHttpsPort(int httpsPort) { this.httpsPort = httpsPort; }

    /**
     * These are required for Hostname Settings
     */
    public boolean isCloudEnabled() { return cloudEnabled; }
    public void setCloudEnabled(boolean cloudEnabled) { this.cloudEnabled = cloudEnabled; }
    public boolean isSupportEnabled() { return supportEnabled; }
    public void setSupportEnabled(boolean supportEnabled) { this.supportEnabled = supportEnabled; }
    public boolean isHttpAdministrationAllowed() { return httpAdministrationAllowed; }
    public void setHttpAdministrationAllowed(boolean httpAdministrationAllowed) { this.httpAdministrationAllowed = httpAdministrationAllowed; }
    public String getAdministrationSubnets() { return administrationSubnets; }
    public void setAdministrationSubnets(String administrationSubnets) { this.administrationSubnets = administrationSubnets; }

    public SnmpSettings getSnmpSettings() { return snmpSettings; }
    public void setSnmpSettings(SnmpSettings snmpSettings) { this.snmpSettings = snmpSettings; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }
    public boolean isDynamicDnsServiceEnabled() { return dynamicDnsServiceEnabled; }
    public void setDynamicDnsServiceEnabled(boolean dynamicDnsServiceEnabled) { this.dynamicDnsServiceEnabled = dynamicDnsServiceEnabled; }
    public String getDynamicDnsServiceName() { return dynamicDnsServiceName; }
    public void setDynamicDnsServiceName(String dynamicDnsServiceName) { this.dynamicDnsServiceName = dynamicDnsServiceName; }
    public String getDynamicDnsServiceUsername() { return dynamicDnsServiceUsername; }
    public void setDynamicDnsServiceUsername(String dynamicDnsServiceUsername) { this.dynamicDnsServiceUsername = dynamicDnsServiceUsername; }
    public String getDynamicDnsServicePassword() { return dynamicDnsServicePassword; }
    public void setDynamicDnsServicePassword(String dynamicDnsServicePassword) { this.dynamicDnsServicePassword = dynamicDnsServicePassword; }
    public String getDynamicDnsServiceZone() { return dynamicDnsServiceZone; }
    public void setDynamicDnsServiceZone(String dynamicDnsServiceZone) { this.dynamicDnsServiceZone = dynamicDnsServiceZone; }
    public String getDynamicDnsServiceHostnames() { return dynamicDnsServiceHostnames; }
    public void setDynamicDnsServiceHostnames(String dynamicDnsServiceHostnames) { this.dynamicDnsServiceHostnames = dynamicDnsServiceHostnames; }
    public String getDynamicDnsServiceWan() { return dynamicDnsServiceWan; }
    public void setDynamicDnsServiceWan(String dynamicDnsServiceWan) { this.dynamicDnsServiceWan = dynamicDnsServiceWan; }
    public String getPublicUrlMethod() { return publicUrlMethod; }
    public void setPublicUrlMethod(String publicUrlMethod) { this.publicUrlMethod = publicUrlMethod; }
    public String getPublicUrlAddress() { return publicUrlAddress; }
    public void setPublicUrlAddress(String publicUrlAddress) { this.publicUrlAddress = publicUrlAddress; }
    public Integer getPublicUrlPort() { return publicUrlPort; }
    public void setPublicUrlPort(Integer publicUrlPort) { this.publicUrlPort = publicUrlPort; }

    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public DayOfWeekMatcher getAutoUpgradeDays() { return autoUpgradeDays; }
    public void setAutoUpgradeDays(DayOfWeekMatcher autoUpgradeDays) { this.autoUpgradeDays = autoUpgradeDays; }
    public int getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(int hourOfDay) { this.hourOfDay = hourOfDay; }
    public int getMinuteOfHour() { return minuteOfHour; }
    public void setMinuteOfHour(int minuteOfHour) { this.minuteOfHour = minuteOfHour; }

    /**
     * These are required for Local Directory Settings
     */
    public boolean isRadiusServerEnabled() { return radiusServerEnabled; } 
    public void setRadiusServerEnabled(boolean radiusServerEnabled) { this.radiusServerEnabled = radiusServerEnabled; }
    public String getRadiusServerSecret() { return radiusServerSecret; }
    public void setRadiusServerSecret(String radiusServerSecret) { this.radiusServerSecret = radiusServerSecret; } 
    public boolean isRadiusProxyEnabled() { return radiusProxyEnabled; }
    public void setRadiusProxyEnabled(boolean radiusProxyEnabled) { this.radiusProxyEnabled = radiusProxyEnabled; }
    public String getRadiusProxyServer() { return radiusProxyServer; }
    public void setRadiusProxyServer(String radiusProxyServer) { this.radiusProxyServer = radiusProxyServer; }
    public String getRadiusProxyWorkgroup() { return radiusProxyWorkgroup; }
    public void setRadiusProxyWorkgroup(String radiusProxyWorkgroup) { this.radiusProxyWorkgroup = radiusProxyWorkgroup; }
    public String getRadiusProxyRealm() { return radiusProxyRealm; }
    public void setRadiusProxyRealm(String radiusProxyRealm) { this.radiusProxyRealm = radiusProxyRealm; }
    public String getRadiusProxyUsername() { return radiusProxyUsername; }
    public void setRadiusProxyUsername(String radiusProxyUsername) { this.radiusProxyUsername = radiusProxyUsername; }
    public String getRadiusProxyPassword() { return radiusProxyPassword; }
    public void setRadiusProxyPassword(String radiusProxyPassword) { this.radiusProxyPassword = radiusProxyPassword; }
    public String getRadiusProxyEncryptedPassword() { return radiusProxyEncryptedPassword; }
    public void setRadiusProxyEncryptedPassword(String radiusProxyEncryptedPassword) { this.radiusProxyEncryptedPassword = radiusProxyEncryptedPassword; }

    public String getWebCertificate() { return webCertificate; }
    public void setWebCertificate(String webCertificate) { this.webCertificate = webCertificate; }
    public String getMailCertificate() { return mailCertificate; }
    public void setMailCertificate(String mailCertificate) { this.mailCertificate = mailCertificate; }
    public String getIpsecCertificate() { return ipsecCertificate; }
    public void setIpsecCertificate(String ipsecCertificate) { this.ipsecCertificate = ipsecCertificate; }
    public String getRadiusCertificate() { return radiusCertificate; }
    public void setRadiusCertificate(String radiusCertificate) { this.radiusCertificate = radiusCertificate; }

    public LanguageSettings getLanguageSettings() { return languageSettings;}
    public void setLanguageSettings(LanguageSettings languageSettings) { this.languageSettings = languageSettings; }
    public LinkedList<LocaleInfo> getLanguagesList() { return languagesList; }
    public void setLanguagesList(LinkedList<LocaleInfo> languagesList) { this.languagesList = languagesList; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Populates the provided {@link SystemSettings}, {@link NetworkSettings} and {@link LanguageSettings}
     * instance with data from this {@link SystemSettingsGeneric} instance.
     * @param systemSettings the target {@link SystemSettings} object to be updated.
     * @param networkSettings the target {@link NetworkSettings} object to be updated.
     * @param languageSettings the target {@link LanguageSettings} object to be updated.
     */
    public void transformGenericToLegacySettings(SystemSettings systemSettings, NetworkSettings networkSettings, LanguageSettings languageSettings) {
        if(networkSettings != null) {
            // Local Services Settings
            networkSettings.setHttpPort(this.getHttpPort());
            networkSettings.setHttpsPort(this.getHttpsPort());

            // Hostname Settings
            networkSettings.setHostName(this.hostName);
            networkSettings.setDomainName(this.domainName);

            networkSettings.setDynamicDnsServiceEnabled(this.dynamicDnsServiceEnabled);
            networkSettings.setDynamicDnsServiceUsername(this.dynamicDnsServiceUsername);
            networkSettings.setDynamicDnsServiceZone(this.dynamicDnsServiceZone);
            networkSettings.setDynamicDnsServiceName(this.dynamicDnsServiceName);
            networkSettings.setDynamicDnsServiceHostnames(this.dynamicDnsServiceHostnames);
            networkSettings.setDynamicDnsServicePassword(this.dynamicDnsServicePassword);
            networkSettings.setDynamicDnsServiceWan(this.dynamicDnsServiceWan);

            networkSettings.setPublicUrlAddress(this.publicUrlAddress);
            networkSettings.setPublicUrlMethod(this.publicUrlMethod);
            networkSettings.setPublicUrlPort(this.publicUrlPort);
        }

        if (systemSettings != null) {
            systemSettings.setCloudEnabled(this.isCloudEnabled());
            systemSettings.setSupportEnabled(this.isSupportEnabled());
            systemSettings.setHttpAdministrationAllowed(this.isHttpAdministrationAllowed());
            systemSettings.setAdministrationSubnets(this.getAdministrationSubnets());
            systemSettings.setLogRetention(this.getLogRetention());
            systemSettings.setThresholdTemperature(this.getThresholdTemperature());
            systemSettings.setSnmpSettings(this.getSnmpSettings() != null ? this.getSnmpSettings() : new SnmpSettings());
            systemSettings.setAutoUpgrade(this.enabled);
            systemSettings.setAutoUpgradeDays(this.autoUpgradeDays);
            systemSettings.setAutoUpgradeHour(this.hourOfDay);
            systemSettings.setAutoUpgradeMinute(this.minuteOfHour);
            systemSettings.setRadiusServerEnabled(this.radiusServerEnabled);
            systemSettings.setRadiusServerSecret(this.radiusServerSecret);
            systemSettings.setRadiusProxyEnabled(this.radiusProxyEnabled);
            systemSettings.setRadiusProxyServer(this.radiusProxyServer);
            systemSettings.setRadiusProxyWorkgroup(this.radiusProxyWorkgroup);
            systemSettings.setRadiusProxyRealm(this.radiusProxyRealm);
            systemSettings.setRadiusProxyUsername(this.radiusProxyUsername);
            systemSettings.setRadiusProxyPassword(this.radiusProxyPassword);
            systemSettings.setRadiusProxyEncryptedPassword(this.radiusProxyEncryptedPassword);
            systemSettings.setWebCertificate(this.webCertificate);
            systemSettings.setMailCertificate(this.mailCertificate);
            systemSettings.setIpsecCertificate(this.ipsecCertificate);
            systemSettings.setRadiusCertificate(this.radiusCertificate);
        }

        if (languageSettings != null && this.getLanguageSettings() != null) {
            boolean isDefaultFormat = this.getLanguageSettings().getRegionalFormats().equals("default");
            String[] languageSplit = this.getLanguageSettings().getLanguage().split(Constants.HYPHEN);

            if(!languageSplit[0].equals("official")) {
                // Something bad has happened; refer to known good language.
                languageSplit[0] = "official";
                languageSplit[1] = "en";
            }
            languageSettings.setSource(languageSplit[0]);
            languageSettings.setLanguage(languageSplit[1]);
            languageSettings.setRegionalFormats(this.getLanguageSettings().getRegionalFormats());
            languageSettings.setOverrideDecimalSep(isDefaultFormat ? StringUtils.EMPTY : this.getLanguageSettings().getOverrideDecimalSep());
            languageSettings.setOverrideThousandSep(isDefaultFormat ? StringUtils.EMPTY : this.getLanguageSettings().getOverrideThousandSep());
            languageSettings.setOverrideDateFmt(isDefaultFormat ? StringUtils.EMPTY : this.getLanguageSettings().getOverrideDateFmt());
            languageSettings.setOverrideTimestampFmt(isDefaultFormat ? StringUtils.EMPTY : this.getLanguageSettings().getOverrideTimestampFmt());
            languageSettings.setLastSynchronized(this.getLanguageSettings().getLastSynchronized());
        }
    }
    /**
     * Represents the TimeZone data structure received from the Vue UI.
     */
        public static class TimeZone implements Serializable{
        private String displayName= StringUtils.EMPTY;
        private String value = StringUtils.EMPTY;

        public TimeZone() {
        }

        public TimeZone(String displayName, String value) {
            this.displayName = displayName;
            this.value = value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
