/**
 * $Id$
 */
package com.untangle.app.directory_connector;

import org.json.JSONObject;
import org.json.JSONString;
import java.util.List;
import java.util.LinkedList;

/**
 * Settings for the Active Directory (really a bunch of LDAP settings).
 */
@SuppressWarnings("serial")
public class ActiveDirectorySettings implements java.io.Serializable, JSONString
{
    private boolean isEnabled = false;
    private List<ActiveDirectoryServer> servers = new LinkedList<>();

    /**
     * Returns true if active directory  is enabled.
     *
     * @return true if active directory is enabled, otherwise false
     */
    public boolean getEnabled() { return isEnabled; }
    /**
     * Sets whether active directory is enabled.
     *
     * @param isEnabled true if active directory is enabled, otherwise false
     */
    public void setEnabled(boolean isEnabled) { this.isEnabled = isEnabled; }

    /**
     * Returns list of active directory servers.
     *
     * @return List of ActiveDirectoryServer for all defined AD servers.
     */
    public List<ActiveDirectoryServer> getServers() { return servers; }
    /**
     * Sets list of active directory servers.
     *
     * @param servers List of ActiveDirectoryServer for all defined AD servers.
     */
    public void setServers( List<ActiveDirectoryServer> servers ) { this.servers = servers; }

    /**
    * Constructor
    */
    public ActiveDirectorySettings() { }

    /**
     * Everything below will be removed.
     */
    private String superuser;
    private String superuserPass;
    private String domain;
    private String ldapHost;
    private boolean ldapSecure;
    private String ouFilter = "";
    private List<String> ouFilters = new LinkedList<>();
    private int ldapPort;


    public ActiveDirectorySettings(String superuser, String superuserPass, String domain, String ldapHost, int ldapPort, boolean ldapSecure)
    {
        this.superuser = superuser;
        this.superuserPass = superuserPass;
        this.domain = domain;
        this.ldapHost = ldapHost;
        this.ldapPort = ldapPort;
        this.ldapSecure = ldapSecure;
        this.ouFilters = new LinkedList<>();
    }

    public String getSuperuser() { return superuser; }
    public void setSuperuser(String dn) { this.superuser = dn; }

    public String getSuperuserPass() { return superuserPass; }
    public void setSuperuserPass(String pass) { this.superuserPass = pass; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getLDAPHost() { return ldapHost; }
    public void setLDAPHost(String ldapHost) { this.ldapHost = ldapHost; }

    public int getLDAPPort() { return ldapPort; }
    public void setLDAPPort(int port) { this.ldapPort = port; }

    public boolean getLDAPSecure() { return ldapSecure; }
    public void setLDAPSecure(boolean secure) { this.ldapSecure = secure; }

    public String getOUFilter() { return ouFilter; }
    public void setOUFilter(String ouFilter) { this.ouFilter = ouFilter; }

    public List<String> getOUFilters() { return ouFilters; }
    public void setOUFilters(List<String> ouFilters) { this.ouFilters = ouFilters; }

    /**
     * Returns settings as a JSON string.
     *
     * @return
     *      Server settings in JSON form.
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
