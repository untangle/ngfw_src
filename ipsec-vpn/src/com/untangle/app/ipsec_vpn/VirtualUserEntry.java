/**
 * $Id: VirtualUserEntry.java 37267 2014-02-26 23:42:19Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.net.InetAddress;

/**
 * Class to store details for active IPsec VPN (L2TP | XAUTH | IKEv2) users.
 * 
 * @author mahotz
 * 
 */
public class VirtualUserEntry
{
    private String clientProtocol;
    private InetAddress clientAddress;
    private String clientUsername;
    private String netInterface;
    private String netProcess;
    private long sessionCreation;
    private VirtualUserEvent eventHolder;

    /**
     * Constructor
     * 
     * @param clientProtocol
     *        The client VPN protocol
     * @param clientAddress
     *        The client address
     * @param clientUsername
     *        The client username
     * @param netInterface
     *        The client interface
     * @param netProcess
     *        The network process
     */
    public VirtualUserEntry(String clientProtocol, InetAddress clientAddress, String clientUsername, String netInterface, String netProcess)
    {
        this.clientProtocol = clientProtocol;
        this.clientAddress = clientAddress;
        this.clientUsername = clientUsername;
        this.netInterface = netInterface;
        this.netProcess = netProcess;
        sessionCreation = System.currentTimeMillis();
    }

    /**
     * @return The client VPN protocol
     */
    public String getClientProtocol()
    {
        return (clientProtocol);
    }

    /**
     * @return The client address
     */
    public InetAddress getClientAddress()
    {
        return (clientAddress);
    }

    /**
     * @return The client username
     */
    public String getClientUsername()
    {
        return (clientUsername);
    }

    /**
     * @return The client interface
     */
    public String getNetInterface()
    {
        return (netInterface);
    }

    /**
     * @return The network process
     */
    public String getNetProcess()
    {
        return (netProcess);
    }

    /**
     * @return The session creation time
     */
    public long getSessionCreation()
    {
        return (sessionCreation);
    }

    /**
     * @return The session elapsed time
     */
    public long getSessionElapsed()
    {
        return (System.currentTimeMillis() - sessionCreation);
    }

    /**
     * Saves the log event created when a user connects
     * 
     * @param event
     *        The log event to be updated on disconnect
     */
    public void pushEventHolder(VirtualUserEvent event)
    {
        this.eventHolder = event;
    }

    /**
     * @return The log event saved when the user connected
     */
    public VirtualUserEvent grabEventHolder()
    {
        return (eventHolder);
    }
}
