package com.untangle.node.cpd;

import java.io.Serializable;
import java.net.InetAddress;
import java.sql.Timestamp;

@SuppressWarnings("serial")
public class HostDatabaseEntry implements Serializable
{
    private Long id;
    private String hardwareAddress;
    private InetAddress ipv4Address;
    private String username;
    private Timestamp lastSession;
    private Timestamp sessionStart;
    private Timestamp expirationDate;

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

    public Timestamp getLastSession()
    {
        return lastSession;
    }

    public void setLastSession(Timestamp lastSession)
    {
        this.lastSession = lastSession;
    }

    public Timestamp getSessionStart()
    {
        return sessionStart;
    }

    public void setSessionStart(Timestamp sessionStart)
    {
        this.sessionStart = sessionStart;
    }

    public Timestamp getExpirationDate()
    {
        return expirationDate;
    }

    public void setExpirationDate(Timestamp expirationDate)
    {
        this.expirationDate = expirationDate;
    }
}
