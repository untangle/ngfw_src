/**
 * $Id$
 */
package com.untangle.uvm.netcap;

import java.net.InetAddress;

import com.untangle.jnetcap.Endpoint;
import com.untangle.jnetcap.Endpoints;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.engine.NodeSessionImpl;

public abstract class NetcapIPNewSessionRequest extends NetcapNewSessionRequest
{
    public static final byte REQUESTED = 2;
    public static final byte REJECTED = 99;
    public static final byte RELEASED = 98;
    public static final byte ENDPOINTED = 100;
    public static final byte REJECTED_SILENT = 101;

    // Codes for rejectReturnUnreachable() and for reset
    static final byte NET_UNREACHABLE = 0;
    static final byte HOST_UNREACHABLE = 1;
    static final byte PROTOCOL_UNREACHABLE = 2;
    static final byte PORT_UNREACHABLE = 3;
    // static final byte DEST_NETWORK_UNKNOWN = 6;  // By RFC1812, should use NET_UNREACHABLE instead
    static final byte DEST_HOST_UNKNOWN = 7;
    // static final byte PROHIBITED_NETWORK = 9;    // By RFC1812, should use PROHIBITED instead
    // static final byte PROHIBITED_HOST = 10;      // By RFC1812, should use PROHIBITED instead
    static final byte PROHIBITED = 13;
    // Only valid for TCP connections
    static final byte TCP_REJECT_RESET = 64;

    protected InetAddress clientAddr;
    protected int clientPort;
    protected int clientIntf;

    protected InetAddress serverAddr;
    protected int serverPort;
    protected int serverIntf;

    protected final InetAddress natFromHost;
    protected final int natFromPort;
    protected final InetAddress natToHost;
    protected final int natToPort;

    protected SessionEvent sessionEvent;

    protected byte state = REQUESTED;

    /* This is used to distinguish between REJECTED and REJECTED with code */
    protected byte code  = REJECTED;

    /* Two ways to create an IPNewSessionRequest:
     * A. Pass in the netcap session and get the parameters from there.
     */
    public NetcapIPNewSessionRequest( SessionGlobalState sessionGlobalState, PipelineAgent agent, SessionEvent pe )
    {
        super( sessionGlobalState, agent );

        Endpoints clientSide = sessionGlobalState.netcapSession().clientSide();
        Endpoints serverSide = sessionGlobalState.netcapSession().serverSide();

        Endpoint client = clientSide.client();
        Endpoint server = clientSide.server();

        /* Get the server and client from the client end of the endpoint from the netcap session */
        clientAddr = client.host();
        clientPort = client.port();

        clientIntf = clientSide.interfaceId();
        serverIntf = serverSide.interfaceId();
        this.sessionEvent = pe;

        serverAddr = server.host();
        serverPort = server.port();

        natFromHost = sessionGlobalState.netcapSession.natInfo.fromHost;
        natFromPort = sessionGlobalState.netcapSession.natInfo.fromPort;
        natToHost = sessionGlobalState.netcapSession.natInfo.toHost;
        natToPort = sessionGlobalState.netcapSession.natInfo.toPort;
    }

    /* Two ways to create an IPNewSessionRequest:
     * B. Pass in the previous request and get the parameters from there
     */
    public NetcapIPNewSessionRequest( NodeSession session, PipelineAgent agent, SessionEvent pe, SessionGlobalState sessionGlobalState)
    {
        super( ((NodeSessionImpl)session).sessionGlobalState(), agent);

        /* Get the server and client from the previous request */
        clientAddr = session.getClientAddr();
        clientPort = session.getClientPort();
        clientIntf = session.getClientIntf();

        serverAddr = session.getServerAddr();
        serverPort = session.getServerPort();
        serverIntf = session.getServerIntf();

        natFromHost = sessionGlobalState.netcapSession.natInfo.fromHost;
        natFromPort = sessionGlobalState.netcapSession.natInfo.fromPort;
        natToHost = sessionGlobalState.netcapSession.natInfo.toHost;
        natToPort = sessionGlobalState.netcapSession.natInfo.toPort;

        this.sessionEvent = pe;
    }

    public short getProtocol()
    {
        return sessionGlobalState.getProtocol();
    }

    public InetAddress getClientAddr()
    {
        return clientAddr;
    }

    public void setClientAddr( InetAddress addr )
    {
        clientAddr = addr;
    }

    public int getClientPort()
    {
        return clientPort;
    }

    public void setClientPort( int port )
    {
        clientPort = port;
    }

    public int getClientIntf()
    {
        return clientIntf;
    }

    public InetAddress getServerAddr()
    {
        return serverAddr;
    }

    public void setServerAddr( InetAddress addr )
    {
        serverAddr = addr;
    }

    public int getServerPort()
    {
        return serverPort;
    }

    public void setServerPort( int port )
    {
        serverPort = port;
    }

    public int getServerIntf()
    {
        return serverIntf;
    }

    public SessionEvent sessionEvent()
    {
        return sessionEvent;
    }

    // One of REQUESTED, REJECTED, RELEASED
    public byte state()
    {
        return state;
    }

    public byte rejectCode()
    {
        return code;
    }

    // May only be called before session is established (from UDPNewSessionRequestEvent handler)
    public void rejectSilently()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is not in the requested state" );
        }

        state = REJECTED_SILENT;
    }

    // May only call if the session is in the requested state
    public void endpoint()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is not in the requested state" );
        }

        state = ENDPOINTED;
    }

    // May only be called before session is established (from UDPNewSessionRequestEvent handler)
    public void rejectReturnUnreachable( byte code )
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is in the state: " + state );
        }

        switch ( code ) {
        case NET_UNREACHABLE:
        case HOST_UNREACHABLE:
        case PROTOCOL_UNREACHABLE:
        case PORT_UNREACHABLE:
        case DEST_HOST_UNKNOWN:
        case PROHIBITED:
            state     = REJECTED;
            this.code = code;
            break;

        default:
            throw new IllegalArgumentException( "Invalid code: " + code );
        }
    }

    // May only be called before session is established (from TCPNewSessionRequestEvent handler)
    public void release()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Can't release a session that is in the state: " + state );
        }

        state = RELEASED;
    }

    public InetAddress getNatFromHost()
    {
	return natFromHost;
    }

    public int getNatFromPort()
    {
	return natFromPort;
    }

    public InetAddress getNatToHost()
    {
	return natToHost;
    }

    public int getNatToPort()
    {
	return natToPort;
    }
}
