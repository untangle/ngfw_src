/**
 * $Id$
 */
package com.untangle.node.spam;

public class DnsblClientContext
{
    private String hostname;
    private String ipAddr;
    private String invertedIPAddr;

    private volatile Boolean isBlacklisted = null;

    public DnsblClientContext(String hostname, String ipAddr, String invertedIPAddr)
    {
        this.hostname = hostname;
        this.ipAddr = ipAddr;
        this.invertedIPAddr = invertedIPAddr;
    }

    public String getHostname()
    {
        return hostname;
    }

    public String getIPAddr()
    {
        return ipAddr;
    }

    public String getInvertedIPAddr()
    {
        return invertedIPAddr;
    }

    public void setResult(Boolean isBlacklisted)
    {
        this.isBlacklisted = isBlacklisted;
        return;
    }

    public Boolean getResult()
    {
        return isBlacklisted;
    }
}
