/**
 * $Id$
 */
package com.untangle.uvm.generic;

import com.untangle.uvm.SystemSettings;
import com.untangle.uvm.network.NetworkSettings;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Generic (v2) System Settings.
 */
@SuppressWarnings("serial")
public class SystemSettingsGeneric implements Serializable, JSONString {

    /**
     * Refers to {@link com.untangle.uvm.UvmContextImpl#isCCHidden()} .
     */
    private boolean isCCHidden;

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
    public boolean isCloudEnabled() {
        return cloudEnabled;
    }

    public void setCloudEnabled(boolean cloudEnabled) {
        this.cloudEnabled = cloudEnabled;
    }

    public boolean isSupportEnabled() {
        return supportEnabled;
    }

    public void setSupportEnabled(boolean supportEnabled) {
        this.supportEnabled = supportEnabled;
    }
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

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Populates the provided {@link SystemSettings} and {@link NetworkSettings} instance with data from this
     * {@link SystemSettingsGeneric} instance.
     * @param systemSettings the target {@link SystemSettings} object to be updated.
     * @param networkSettings the target {@link NetworkSettings} object to be updated.
     */
    public void transformGenericToLegacySettings(SystemSettings systemSettings, NetworkSettings networkSettings) {
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
