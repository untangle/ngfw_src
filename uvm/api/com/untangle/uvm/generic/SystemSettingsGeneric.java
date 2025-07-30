/**
 * $Id$
 */
package com.untangle.uvm.generic;

import com.untangle.uvm.SystemSettings;
import com.untangle.uvm.network.NetworkSettings;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;

/**
 * Generic (v2) System Settings.
 */
@SuppressWarnings("serial")
public class SystemSettingsGeneric implements Serializable, JSONString {

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
    }
}
