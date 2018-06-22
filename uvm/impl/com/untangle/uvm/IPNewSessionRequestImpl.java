/**
 * $Id$
 */

package com.untangle.uvm;

import java.util.HashMap;
import java.util.Map;
import java.net.InetAddress;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.Endpoints;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.IPNewSessionRequest;

/**
 * Class for representing IP new session requests
 */
public abstract class IPNewSessionRequestImpl implements IPNewSessionRequest
{
    public static final byte REQUESTED = 2;
    public static final byte REJECTED = 99;
    public static final byte RELEASED = 98;
    public static final byte ENDPOINTED = 100;
    public static final byte REJECTED_SILENT = 101;

    // Codes for rejectReturnUnreachable() and for reset
    public static final byte NET_UNREACHABLE = 0;
    public static final byte HOST_UNREACHABLE = 1;
    public static final byte PROTOCOL_UNREACHABLE = 2;
    public static final byte PORT_UNREACHABLE = 3;
    //public  static final byte DEST_NETWORK_UNKNOWN = 6;  // By RFC1812, should use NET_UNREACHABLE instead
    public static final byte DEST_HOST_UNKNOWN = 7;
    //public  static final byte PROHIBITED_NETWORK = 9;    // By RFC1812, should use PROHIBITED instead
    //public  static final byte PROHIBITED_HOST = 10;      // By RFC1812, should use PROHIBITED instead
    public static final byte PROHIBITED = 13;
    public static final byte TCP_REJECT_RESET = 64; // Only valid for TCP connections

    protected final PipelineConnectorImpl pipelineConnector;
    protected final SessionGlobalState sessionGlobalState;
    protected final SessionEvent sessionEvent;

    protected final int clientIntf;
    protected final int serverIntf;

    protected final InetAddress origClientAddr;
    protected final int origClientPort;
    protected final InetAddress origServerAddr;
    protected final int origServerPort;

    protected InetAddress newClientAddr;
    protected int newClientPort;
    protected InetAddress newServerAddr;
    protected int newServerPort;

    protected byte state = REQUESTED; /* REQUESTED, REJECTED, RELEASED */
    protected byte rejectCode = REJECTED;

    protected HashMap<String, Object> stringAttachments = new HashMap<String, Object>();
    private static final String NO_KEY_VALUE = "NOKEY";

    /**
     * 
     * First way to create an IPNewSessionRequest: Pass in the netcap session
     * and get the parameters from there.
     * 
     * @param sessionGlobalState
     *        The session global state
     * @param connector
     *        The pipeline connector
     * @param sessionEvent
     *        The session event
     */
    public IPNewSessionRequestImpl(SessionGlobalState sessionGlobalState, PipelineConnectorImpl connector, SessionEvent sessionEvent)
    {
        this.sessionGlobalState = sessionGlobalState;
        this.pipelineConnector = connector;
        this.sessionEvent = sessionEvent;

        Endpoints clientSide = sessionGlobalState.netcapSession().clientSide();
        Endpoints serverSide = sessionGlobalState.netcapSession().serverSide();

        clientIntf = clientSide.interfaceId();
        serverIntf = serverSide.interfaceId();

        origClientAddr = clientSide.client().host();
        origClientPort = clientSide.client().port();
        origServerAddr = clientSide.server().host();
        origServerPort = clientSide.server().port();

        newClientAddr = serverSide.client().host();
        newClientPort = serverSide.client().port();
        newServerAddr = serverSide.server().host();
        newServerPort = serverSide.server().port();
    }

    /**
     * Second way to create an IPNewSessionRequest: Pass in the previous request
     * and get the parameters from there
     * 
     * @param prevRequest
     *        The previous request
     * @param connector
     *        The pipeline connector
     * @param sessionEvent
     *        The session event
     * @param sessionGlobalState
     *        The session global state
     */
    public IPNewSessionRequestImpl(IPNewSessionRequestImpl prevRequest, PipelineConnectorImpl connector, SessionEvent sessionEvent, SessionGlobalState sessionGlobalState)
    {
        this.sessionGlobalState = sessionGlobalState;
        this.pipelineConnector = connector;
        this.sessionEvent = sessionEvent;

        Endpoints clientSide = sessionGlobalState.netcapSession().clientSide();
        Endpoints serverSide = sessionGlobalState.netcapSession().serverSide();

        clientIntf = clientSide.interfaceId();
        serverIntf = serverSide.interfaceId();

        origClientAddr = clientSide.client().host();
        origClientPort = clientSide.client().port();
        origServerAddr = clientSide.server().host();
        origServerPort = clientSide.server().port();

        /**
         * get the new tuple attributes from the previous session in case it was
         * changed
         */
        newClientAddr = prevRequest.getNewClientAddr();
        newClientPort = prevRequest.getNewClientPort();
        newServerAddr = prevRequest.getNewServerAddr();
        newServerPort = prevRequest.getNewServerPort();
    }

    /**
     * Get the pipeline connectors
     * 
     * @return The pipeline connectors
     */
    public PipelineConnectorImpl pipelineConnector()
    {
        return pipelineConnector;
    }

    /**
     * Get the netcap session
     * 
     * @return The netcap session
     */
    public NetcapSession netcapSession()
    {
        return sessionGlobalState.netcapSession();
    }

    /**
     * Get the session global state
     * 
     * @return The session global state
     */
    public SessionGlobalState sessionGlobalState()
    {
        return sessionGlobalState;
    }

    /**
     * Get the ID
     * 
     * @return The ID
     */
    public long id()
    {
        return sessionGlobalState.id();
    }

    /**
     * Get the session ID
     * 
     * @return The session ID
     */
    public long getSessionId()
    {
        return sessionGlobalState.id();
    }

    /**
     * Get the user
     * 
     * @return The user
     */
    public String user()
    {
        return sessionGlobalState.user();
    }

    /**
     * Get the protocol
     * 
     * @return The protocol
     */
    public short getProtocol()
    {
        return sessionGlobalState.getProtocol();
    }

    /**
     * Get the client interface
     * 
     * @return The client interface
     */
    public int getClientIntf()
    {
        return clientIntf;
    }

    /**
     * Get the server interface
     * 
     * @return The server interface
     */
    public int getServerIntf()
    {
        return serverIntf;
    }

    /**
     * Get the original client address
     * 
     * @return The original client address
     */
    public InetAddress getOrigClientAddr()
    {
        return origClientAddr;
    }

    /**
     * Get the original client port
     * 
     * @return The original client port
     */
    public int getOrigClientPort()
    {
        return origClientPort;
    }

    /**
     * Get the new client address
     * 
     * @return The new client address
     */
    public InetAddress getNewClientAddr()
    {
        return newClientAddr;
    }

    /**
     * Get the new client port
     * 
     * @return The new client port
     */
    public int getNewClientPort()
    {
        return newClientPort;
    }

    /**
     * Get the original server address
     * 
     * @return The original server address
     */
    public InetAddress getOrigServerAddr()
    {
        return origServerAddr;
    }

    /**
     * Get the original server port
     * 
     * @return The original server port
     */
    public int getOrigServerPort()
    {
        return origServerPort;
    }

    /**
     * Get the new server address
     * 
     * @return The new server address
     */
    public InetAddress getNewServerAddr()
    {
        return newServerAddr;
    }

    /**
     * Get the new server port
     * 
     * @return The new server port
     */
    public int getNewServerPort()
    {
        return newServerPort;
    }

    /**
     * Set the new client address
     * 
     * @param newValue
     *        The new client address
     */
    public void setNewClientAddr(InetAddress newValue)
    {
        this.newClientAddr = newValue;
    }

    /**
     * Set the new client port
     * 
     * @param newValue
     *        The new client port
     */
    public void setNewClientPort(int newValue)
    {
        this.newClientPort = newValue;
    }

    /**
     * Set the new server address
     * 
     * @param newValue
     *        The new server address
     */
    public void setNewServerAddr(InetAddress newValue)
    {
        this.newServerAddr = newValue;
    }

    /**
     * Set the new server port
     * 
     * @param newValue
     *        The new server port
     */
    public void setNewServerPort(int newValue)
    {
        this.newServerPort = newValue;
    }

    /**
     * Get the session event
     * 
     * @return The session event
     */
    public SessionEvent sessionEvent()
    {
        return sessionEvent;
    }

    /**
     * Get the state
     * 
     * @return The state
     */
    public byte state()
    {
        return state;
    }

    /**
     * Get the reject code
     * 
     * @return The reject code
     */
    public byte rejectCode()
    {
        return rejectCode;
    }

    /**
     * Reject a session silently
     */
    public void rejectSilently()
    {
        if (state != REQUESTED) {
            throw new IllegalStateException("Unable to reject session that is not in the requested state");
        }

        state = REJECTED_SILENT;
    }

    /**
     * endpoint
     */
    public void endpoint()
    {
        if (state != REQUESTED) {
            throw new IllegalStateException("Unable to reject session that is not in the requested state");
        }

        state = ENDPOINTED;
    }

    /**
     * Reject and return unreachable
     * 
     * @param code
     *        The unreachable code
     */
    public void rejectReturnUnreachable(byte code)
    {
        if (state != REQUESTED) {
            throw new IllegalStateException("Unable to reject session that is in the state: " + state);
        }

        switch (code)
        {
        case NET_UNREACHABLE:
        case HOST_UNREACHABLE:
        case PROTOCOL_UNREACHABLE:
        case PORT_UNREACHABLE:
        case DEST_HOST_UNKNOWN:
        case PROHIBITED:
            state = REJECTED;
            this.rejectCode = code;
            break;

        default:
            throw new IllegalArgumentException("Invalid code: " + code);
        }
    }

    /**
     * Release the session
     */
    public void release()
    {
        if (state != REQUESTED) {
            throw new IllegalStateException("Can't release a session that is in the state: " + state);
        }

        state = RELEASED;
    }

    /**
     * Attach an unnamed object to the session
     * 
     * @param ob
     *        The object
     * @return The object
     */
    public Object attach(Object ob)
    {
        return attach(NO_KEY_VALUE, ob);
    }

    /**
     * Get the unnamed object attached to the session
     * 
     * @return The object
     */
    public Object attachment()
    {
        return attachment(NO_KEY_VALUE);
    }

    /**
     * Attach a named object to the session
     * 
     * @param key
     *        The name
     * @param ob
     *        The object
     * @return The object
     */
    public Object attach(String key, Object ob)
    {
        return this.stringAttachments.put(key, ob);
    }

    /**
     * Get the named object attached to the session
     * 
     * @param key
     *        The name
     * @return The object
     */
    public Object attachment(String key)
    {
        return this.stringAttachments.get(key);
    }

    /**
     * Copy attachments to another session
     * 
     * @param session
     *        The session
     */
    public void copyAttachments(AppSessionImpl session)
    {
        for (Map.Entry<String, Object> entry : stringAttachments.entrySet()) {
            session.attach(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Attach a named global object to the session
     * 
     * @param key
     *        The name
     * @param ob
     *        The object
     * @return The object
     */
    public Object globalAttach(String key, Object ob)
    {
        return this.sessionGlobalState().attach(key, ob);
    }

    /**
     * Get a named global object attached to the session
     * 
     * @param key
     *        The name
     * @return The object
     */
    public Object globalAttachment(String key)
    {
        return this.sessionGlobalState().attachment(key);
    }

    /**
     * Get a string representation
     * 
     * @return The string
     */
    public String toString()
    {
        return "NewSessionRequest: " + getProtocol() + "|" + getOrigClientAddr().getHostAddress() + ":" + getOrigClientPort() + " -> " + getNewServerAddr().getHostAddress() + ":" + getNewServerPort();
    }
}
