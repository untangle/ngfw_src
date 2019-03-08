/**
 * $Id$
 */
package com.untangle.app.directory_connector;

import org.json.JSONObject;
import org.json.JSONString;
import java.util.List;
import java.util.LinkedList;

/**
 * Active Directory server settings.
 *
 * @see
 *      ActiveDirectorySettings
 */
@SuppressWarnings("serial")
public class ActiveDirectoryServer implements java.io.Serializable, JSONString
{
    private String superuser;
    private String superuserPass;
    private String domain;
    private String ldapHost;
    private boolean ldapSecure;
    private List<String> ouFilters = new LinkedList<>();
    private int ldapPort;
    private boolean isEnabled = false;
    private boolean azure = false;

    /**
     * Initialize active directory server.
     */
    public ActiveDirectoryServer() { }

    /**
     * Standard constructor
     *
     * @param superuser
     *      Server username
     * @param superuserPass
     *      Server username password
     * @param domain
     *      AD Domain
     * @param ldapHost
     *      AD hostname
     * @param ldapPort
     *      AD port.  363 for non-secure, 636 for secure.
     * @param ldapSecure
     *      true to access securely, false to use non-secure mode.
     * @param ouFilters
     *      Filters to use in search.
     * @param azure
     *      true if Azure enable, otherwise false.
     */
    public ActiveDirectoryServer(String superuser, String superuserPass, String domain, String ldapHost, int ldapPort, boolean ldapSecure, List<String> ouFilters, boolean azure)
    {
        this.superuser = superuser;
        this.superuserPass = superuserPass;
        this.domain = domain;
        this.ldapHost = ldapHost;
        this.ldapPort = ldapPort;
        this.ldapSecure = ldapSecure;
        this.ouFilters = new LinkedList<>();
        this.azure = azure;
    }

    /**
     * Returns true if azure is enabled.
     *
     * @return
     *      true if azure is enabled, otherwise false
     */
    public boolean getEnabled() { return isEnabled; }
    /**
     * Sets whether azure is enabled.
     *
     * @param isEnabled
     *      true if azure is enabled, otherwise false
     */
    public void setEnabled(boolean isEnabled) { this.isEnabled = isEnabled; }

    /**
     * Returns true if server is enabled.
     *
     * @return
     *      true if server is enabled, otherwise false
     */
    public boolean getAzure() { return azure; }
    /**
     * Sets whether server is enabled.
     *
     * @param azure
     *      true if server is enabled, otherwise false
     */
    public void setAzure(boolean azure) { this.azure = azure; }



    /**
     * Returns superuser username.
     *
     * @return
     *      superuser username.
     */
    public String getSuperuser() { return superuser; }
    /**
     * Sets superuser username.
     *
     * @param dn
     *      superuser username
     */
    public void setSuperuser(String dn) { this.superuser = dn; }

    /**
     * Returns superuser password.
     *
     * @return
     *      superuser password.
     */
    public String getSuperuserPass() { return superuserPass; }
    /**
     * Sets superuser username.
     *
     * @param pass
     *      superuser password
     */
    public void setSuperuserPass(String pass) { this.superuserPass = pass; }

    /**
     * Returns domain name
     *
     * @return
     *      domain name.
     */
    public String getDomain() { return domain; }
    /**
     * Sets domain name
     *
     * @param domain
     *      AD domain name
     */
    public void setDomain(String domain) { this.domain = domain; }

    /**
     * Returns IP reachable server name.
     *
     * @return
     *      IP reachable server name.  This can be a hostname or IP address.
     */
    public String getLDAPHost() { return ldapHost; }
    /**
     * Sets IP reachable server name.
     *
     * @param ldapHost
     *      IP reachable server name.  This can be a hostname or IP address.
     */
    public void setLDAPHost(String ldapHost) { this.ldapHost = ldapHost; }

    /**
     * Returns server TCP port.
     *
     * @return
     *      server TCP port.  Usually 363 for non-secure, 636 for secure.
     */
    public int getLDAPPort() { return ldapPort; }
    /**
     * Sets server TCP port.
     *
     * @param port 
     *      server TCP port.  Usually 363 for non-secure, 636 for secure.
     */
    public void setLDAPPort(int port) { this.ldapPort = port; }

    /**
     * Returns true if secure LDAP is enabled.
     *
     * @return
     *      true if secure LDAP is enabled, otherwise false
     */
    public boolean getLDAPSecure() { return ldapSecure; }
    /**
     * Set true if secure LDAP is enabled.
     *
     * @param secure
     *      true if secure LDAP is enabled, otherwise false
     */
    public void setLDAPSecure(boolean secure) { this.ldapSecure = secure; }

    /**
     * Return OU filters
     *
     * @return
     *      List of OU filters.
     */
    public List<String> getOUFilters() { return ouFilters; }
    /**
     * Set OU filters
     *
     * @param ouFilters
     *      List of OU filters.
     */
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
