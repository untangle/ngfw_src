/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;

import org.json.JSONString;
import org.json.JSONObject;

/**
 * Uri Manager Settings
 */
@SuppressWarnings("serial")
public class UriManagerSettings implements Serializable, JSONString
{
    private Integer version = 2;
    private List<UriTranslation> uriTranslations = new LinkedList<>();

    private String dnsTestHost = "updates.untangle.com";
    private String tcpTestHost = "updates.untangle.com";

    public UriManagerSettings() { }

    public Integer getVersion() { return this.version; }
    public void setVersion( Integer version ) { this.version = version; }

    public List<UriTranslation> getUriTranslations() { return uriTranslations; }
    public void setUriTranslations( List<UriTranslation> uriTranslations ) { this.uriTranslations = uriTranslations; }

    public String getDnsTestHost() { return dnsTestHost; }
    public void setDnsTestHost( String dnsTestHost ) { this.dnsTestHost = dnsTestHost; }

    public String getTcpTestHost() { return tcpTestHost; }
    public void setTcpTestHost( String tcpTestHost ) { this.tcpTestHost = tcpTestHost; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
