/**
 * $Id: VirtualUserEntry.java 37267 2014-02-26 23:42:19Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.net.InetAddress;

public class VirtualUserEntry
{
    private String clientProtocol;
    private InetAddress clientAddress;
    private String clientUsername;
    private String netInterface;
    private String netProcess;
    private long sessionCreation;
    private VirtualUserEvent eventHolder;

    public VirtualUserEntry(String clientProtocol, InetAddress clientAddress, String clientUsername, String netInterface, String netProcess)
    {
        this.clientProtocol = clientProtocol;
        this.clientAddress = clientAddress;
        this.clientUsername = clientUsername;
        this.netInterface = netInterface;
        this.netProcess = netProcess;
        sessionCreation = System.currentTimeMillis();
    }

    public String getClientProtocol()
    {
        return (clientProtocol);
    }

    public InetAddress getClientAddress()
    {
        return (clientAddress);
    }

    public String getClientUsername()
    {
        return (clientUsername);
    }

    public String getNetInterface()
    {
        return (netInterface);
    }

    public String getNetProcess()
    {
        return (netProcess);
    }

    public long getSessionCreation()
    {
        return (sessionCreation);
    }

    public long getSessionElapsed()
    {
        return (System.currentTimeMillis() - sessionCreation);
    }

    public void pushEventHolder(VirtualUserEvent event)
    {
        this.eventHolder = event;
    }

    public VirtualUserEvent grabEventHolder()
    {
        return (eventHolder);
    }
}
