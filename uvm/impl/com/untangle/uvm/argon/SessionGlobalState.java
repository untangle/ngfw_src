/**
 * $Id$
 */
package com.untangle.uvm.argon;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapTCPSession;
import com.untangle.jnetcap.NetcapUDPSession;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * This stores the global system-wide state for a given session
 */
public class SessionGlobalState
{
    private final Logger logger = Logger.getLogger(getClass());

    protected final NetcapSession netcapSession;

    protected final long id;
    protected final short protocol;
    protected String user; // Not final since we can set it later

    protected final SideListener clientSideListener;
    protected final SideListener serverSideListener;

    protected final ArgonHook argonHook;

    protected HashMap<String,Object> attachments;
        
    SessionGlobalState( NetcapSession netcapSession, SideListener clientSideListener, SideListener serverSideListener, ArgonHook argonHook )
    {
        this.argonHook = argonHook;

        this.netcapSession = netcapSession;

        id = netcapSession.id();
        protocol = netcapSession.getProtocol();
        user = null;

        this.clientSideListener = clientSideListener;
        this.serverSideListener = serverSideListener;

        this.attachments = new HashMap<String,Object>();
    }

    public long id()
    {
        return id;
    }
    
    public short getProtocol()
    {
        return protocol;
    }

    public String user()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public NetcapSession netcapSession()
    {
        return netcapSession;
    }

    /**
     * Retrieve the netcap TCP Session.  If this is not a TCP session, this will throw an exception.
     */
    public NetcapTCPSession netcapTCPSession()
    {
        return (NetcapTCPSession)netcapSession;
    }

    /**
     * Retrieve the netcap UDP Session.  If this is not a UDP session, this will throw an exception.
     * XXX Probably better executed with two subclasses.
     */
    public NetcapUDPSession netcapUDPSession()
    {
        return (NetcapUDPSession)netcapSession;
    }

    public SideListener clientSideListener()
    {
        return clientSideListener;
    }

    public SideListener serverSideListener()
    {
        return serverSideListener;
    }

    public ArgonHook argonHook()
    {
        return argonHook;
    }

    public Object attach(String key, Object attachment)
    {
        logger.debug("globalAttach( " + key + " , " + attachment + " )");
        return this.attachments.put(key,attachment);
    }

    public Object attachment(String key)
    {
        return this.attachments.get(key);
    }

    public Map<String,Object> getAttachments()
    {
        return this.attachments;
    }
}
