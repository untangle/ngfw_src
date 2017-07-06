/*
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
    private String superuser;
    private String superuserPass;
    private String domain;
    private String ldapHost;
    private boolean ldapSecure;
    private String ouFilter = "";
    private List<String> ouFilters = null;
    private int ldapPort;
    private boolean isEnabled = false;

    public ActiveDirectorySettings() { }

    public ActiveDirectorySettings(String superuser, String superuserPass, String domain, String ldapHost, int ldapPort, boolean ldapSecure)
    {
        this.superuser = superuser;
        this.superuserPass = superuserPass;
        this.domain = domain;
        this.ldapHost = ldapHost;
        this.ldapPort = ldapPort;
        this.ldapSecure = ldapSecure;
        this.ouFilters = new LinkedList<String>();
    }

    public boolean getEnabled() { return isEnabled; }
    public void setEnabled(boolean isEnabled) { this.isEnabled = isEnabled; }

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

    /* Remove after 13.1 release */
    public String getOUFilter() { return ouFilter; }
    public void setOUFilter(String ouFilter) { this.ouFilter = ouFilter; }

    public List<String> getOUFilters() { return ouFilters; }
    public void setOUFilters(List<String> ouFilters) { this.ouFilters = ouFilters; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
