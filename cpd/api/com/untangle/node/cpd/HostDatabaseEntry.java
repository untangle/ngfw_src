package com.untangle.node.cpd;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

@SuppressWarnings("serial")
public class HostDatabaseEntry implements Serializable
{
    private Long id;
    private String hardwareAddress;
    private InetAddress ipv4Address;
    private String username;
    private Date lastSession;
    private Date sessionStart;
    private Date expirationDate;

    public HostDatabaseEntry()
    {
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getHardwareAddress()
    {
        return hardwareAddress;
    }

    public void setHardwareAddress(String hardwareAddress)
    {
        this.hardwareAddress = hardwareAddress;
    }

    public InetAddress getIpv4Address()
    {
        return ipv4Address;
    }

    public void setIpv4Address(InetAddress ipv4Address)
    {
        this.ipv4Address = ipv4Address;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public Date getLastSession()
    {
        return lastSession;
    }

    public void setLastSession(Date lastSession)
    {
        this.lastSession = lastSession;
    }

    public Date getSessionStart()
    {
        return sessionStart;
    }

    public void setSessionStart(Date sessionStart)
    {
        this.sessionStart = sessionStart;
    }

    public Date getExpirationDate()
    {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate)
    {
        this.expirationDate = expirationDate;
    }
}
